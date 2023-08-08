/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package piuk.blockchain.android.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.icons.ArrowLeft
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.componentlib.viewextensions.windowRect
import com.blockchain.home.presentation.navigation.QrExpected
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.consume
import com.blockchain.utils.unsafeLazy
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.ui.dapps.ConnectedDappsActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.Result
import com.karumi.dexter.Dexter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import java.util.EnumMap
import java.util.EnumSet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityScanBinding
import piuk.blockchain.android.ui.home.QrScanActivityContract
import timber.log.Timber

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a viewfinder to help the
 * user place the barcode correctly, shows feedback as the image processing is happening, and then overlays the results
 * when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
class QrScanActivity : BlockchainActivity(), ScanAndConnectBottomSheet.Host {

    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var camera: Camera? = null
    var targetRect: Rect? = null
    var framingViewSize = Point()

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private val decodeFormats: Collection<BarcodeFormat>? by unsafeLazy {
        intent?.let { parseDecodeFormats(it, productBarcodeFormats, oneDBarcodeFormats) }
    }

    private val decodeHints: Map<DecodeHintType, *>? by unsafeLazy {
        intent?.let { parseDecodeHints(it) }
    }

    private val characterSet: String? by unsafeLazy {
        intent?.getStringExtra(QrScanIntents.CHARACTER_SET)
    }
    private val compositeDisposable = CompositeDisposable()

    private val inactivityTimer = InactivityTimer(this)

    private var flashStatus = false
    private val hasFlashLight: Boolean by unsafeLazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    private val sessionsRepository: SessionRepository by scopedInject()

    private val expectedSet: Set<QrExpected>
        get() = intent?.getParcelableArrayExtra(PARAM_EXPECTED_QR)
            ?.filterIsInstance<QrExpected>()
            ?.toSet() ?: emptySet()

    override val alwaysDisableScreenshots: Boolean = true

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val binding: ActivityScanBinding by lazy {
        ActivityScanBinding.inflate(layoutInflater)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = currentOrientation

        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)
        binding.infoIcon.apply {
            image = ImageResource.Local(R.drawable.ic_information_large)
            onClick = {
                showBottomSheet(ScanAndConnectBottomSheet.newInstance(showCta = false))
            }
            visible()
        }
        binding.backIcon.apply {
            image = Icons.ArrowLeft.withTint(Color.White)
            onClick = {
                finish()
            }
            visible()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupBackPress()
    }

    // handle reverse-mounted cameras on devices like the Nexus 5X
    private val currentOrientation: Int
        get() = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0,
            Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(owner = this) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        inactivityTimer.onActivityResumed()
        if (canOpenScan(this)) {
            start()
        } else {
            requestScanPermissions(
                this,
                { start() },
                { onPermissionDenied(binding.root) }
            )
        }
    }

    private fun doConfigureUiElements() {
        require(expectedSet.isNotEmpty())
        binding.feedbackBlock.visibleIf { QrExpected.WebLoginQr in expectedSet }
        binding.instructions.text = if (expectedSet.size > 1) {
            ""
        } else {
            when (val expect = expectedSet.first()) {
                is QrExpected.AnyAssetAddressQr -> getString(
                    com.blockchain.stringResources.R.string.qr_activity_hint_any_asset
                )

                is QrExpected.AssetAddressQr -> getString(
                    com.blockchain.stringResources.R.string.qr_activity_hint_asset_address,
                    expect.assetTicker
                )

                is QrExpected.BitPayQr -> getString(com.blockchain.stringResources.R.string.qr_activity_hint_bitpay)
                is QrExpected.ImportWalletKeysQr -> getString(
                    com.blockchain.stringResources.R.string.qr_activity_hint_import_wallet
                )

                is QrExpected.WebLoginQr -> getString(
                    com.blockchain.stringResources.R.string.qr_activity_hint_new_web_login
                )

                is QrExpected.WalletConnectQr -> getString(com.blockchain.stringResources.R.string.empty)
            }
        }

        if (expectedSet.contains(QrExpected.WalletConnectQr)) {
            compositeDisposable += sessionsRepository.retrieve()
                .onErrorReturn { emptyList() }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    binding.walletConnectApps.gone()
                }
                .subscribe { apps ->
                    binding.walletConnectApps.visibleIf { apps.isNotEmpty() }
                    binding.walletConnectApps.apply {
                        text =
                            resources.getQuantityString(
                                com.blockchain.stringResources.R.plurals.wallet_connect_connected_apps,
                                apps.size,
                                apps.size
                            )
                        startIcon = ImageResource.Local(R.drawable.ic_vector_world_small)
                        onClick = {
                            startActivity(ConnectedDappsActivity.newIntent(this@QrScanActivity))
                            analytics.logEvent(
                                WalletConnectAnalytics.ConnectedDappsListClicked(
                                    origin = LaunchOrigin.QR_CODE
                                )
                            )
                        }
                    }
                }
        } else {
            binding.walletConnectApps.gone()
        }
    }

    override fun onPause() {
        inactivityTimer.onActivityPaused()
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onDestroy() {
        inactivityTimer.stop()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_CAMERA ->
                // Handle these events so they don't launch the Camera app
                return true

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                camera?.cameraControl?.enableTorch(false)
                return true
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                camera?.cameraControl?.enableTorch(true)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleDecode(rawResult: Result) {
        inactivityTimer.onActivityEvent()
        setResult(
            Activity.RESULT_OK,
            Intent(intent.action).apply {
                putExtra(QrScanActivityContract.EXTRA_SCAN_RESULT, rawResult.toString())
            }
        )
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_flash_light -> {
                toggleTorch(); true
            }

            else -> super.onOptionsItemSelected(item)
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        if (hasFlashLight) {
            menuInflater.inflate(R.menu.menu_scan, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun toggleTorch() {
        try {
            flashStatus = !flashStatus
            camera?.cameraControl?.enableTorch(flashStatus)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras. The exception is caught in onResume().
            lensFacing = when {
                hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.LENS_FACING_BACK
                hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.LENS_FACING_FRONT
                else -> throw java.lang.IllegalStateException("Back and front camera are unavailable")
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    /** Returns true if the device has the specified camera available. False otherwise */
    private fun hasCamera(cameraSelector: CameraSelector): Boolean {
        return cameraProvider?.hasCamera(cameraSelector) ?: false
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val targetWindowRect = binding.viewfinderGuide.windowRect
        binding.viewfinderView.let {
            framingViewSize = Point(it.width, it.height)
            targetRect = targetWindowRect
            it.setTargetRect(targetWindowRect)
        }

        val screenResolution = Point(windowManager.defaultDisplay.width, windowManager.defaultDisplay.height)

        val screenAspectRatio = aspectRatio(screenResolution.x, screenResolution.y)

        val rotation = windowManager.defaultDisplay.rotation

        // CameraProvider, the exception is caught in onResume() where this method is called via setUpCamera().
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    QrCodeAnalyzer(
                        targetRect = targetWindowRect,
                        framingViewSize = framingViewSize,
                        screenResolution = screenResolution,
                        hints = buildsHintsMap(),
                        orientation = resources.configuration.orientation
                    ) { qrResult ->
                        handleDecode(qrResult)
                        binding.previewView.post {
                            finish()
                        }
                    }
                )
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.previewView.surfaceProvider)
            binding.viewfinderView.invalidate()
            binding.viewfinderView.visible()
        } catch (exception: Exception) {
            Timber.e("Unexpected error initializing camera: $exception")
            setResult(RESULT_CAMERA_ERROR)
            finish()
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun buildsHintsMap(): EnumMap<DecodeHintType, Any> {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        decodeHints?.let {
            hints.putAll(it)
        }

        hints[DecodeHintType.ALSO_INVERTED] = true

        // The prefs can't change while the thread is running, so pick them up once here.
        hints[DecodeHintType.POSSIBLE_FORMATS] =
            if (decodeFormats.isNullOrEmpty()) {
                buildFormatSet()
            } else {
                decodeFormats
            }

        characterSet?.let {
            hints[DecodeHintType.CHARACTER_SET] = characterSet
        }

        return hints
    }

    private fun buildFormatSet(): EnumSet<BarcodeFormat> {
        return EnumSet.noneOf(BarcodeFormat::class.java).apply {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@QrScanActivity)
            if (prefs.getBoolean(KEY_DECODE_1D, false)) {
                addAll(oneDBarcodeFormats)
            }
            if (prefs.getBoolean(KEY_DECODE_QR, false)) {
                add(BarcodeFormat.QR_CODE)
            }
            if (prefs.getBoolean(KEY_DECODE_DATA_MATRIX, false)) {
                add(BarcodeFormat.DATA_MATRIX)
            }
        }
    }

    private fun start() {
        // CameraManager must be initialized here, not in onCreate()
        // because possible bugs with setting view when not yet measured
        binding.previewView.post {
            try {
                setUpCamera()
            } catch (e: java.lang.IllegalStateException) {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.camera_setup_failed),
                    type = SnackbarType.Error
                ).show()
                setResult(RESULT_CAMERA_ERROR)
                finish()
            }
        }

        doConfigureUiElements()
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        const val RESULT_CAMERA_ERROR = 10

        private const val PARAM_EXPECTED_QR = "PARAM_EXPECTED_QR"

        const val SCAN_URI_RESULT = 12007

        const val KEY_DECODE_1D = "preferences_decode_1D"
        const val KEY_DECODE_QR = "preferences_decode_QR"
        const val KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix"

        private val productBarcodeFormats: Collection<BarcodeFormat> =
            EnumSet.of(
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14
            )
        private val oneDBarcodeFormats: Collection<BarcodeFormat> =
            EnumSet.of(
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR
            ) + productBarcodeFormats

        fun newInstance(context: Context, expect: Set<QrExpected>) =
            Intent(context, QrScanActivity::class.java).apply {
                action = QrScanIntents.ACTION
                putExtra(QrScanIntents.FORMATS, EnumSet.allOf(BarcodeFormat::class.java).joinToString { it.name })
                putExtra(QrScanIntents.MODE, QrScanIntents.QR_CODE_MODE)
                putExtra(PARAM_EXPECTED_QR, expect.toTypedArray())
            }

        private fun onPermissionDenied(view: View) {
            BlockchainSnackbar.make(
                view,
                view.context.getString(com.blockchain.stringResources.R.string.request_camera_permission),
                type = SnackbarType.Error
            ).show()
        }

        private fun canOpenScan(ctx: Context): Boolean =
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        private fun requestScanPermissions(
            ctx: Context,
            onSuccess: () -> Unit,
            onDenied: () -> Unit
        ) {
            val permissionListener = CameraPermissionListener(
                granted = onSuccess,
                denied = onDenied
            )

            Dexter.withContext(ctx)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(permissionListener)
                .onSameThread()
                .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
                .check()
        }

        fun Intent?.getRawScanData(): String? =
            this?.getStringExtra(QrScanActivityContract.EXTRA_SCAN_RESULT)
    }

    override fun onCameraAccessAllowed() {}

    override fun onSheetClosed() {}
}
