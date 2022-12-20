package piuk.blockchain.android.ui.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.getAlertDialogPaddedView
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.ui.password.SecondPasswordHandler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.MaybeSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import piuk.blockchain.android.R

class ErrorDialogCancelled : Exception("Dialog Cancelled")

class SecondPasswordDialog(
    private val context: Context,
    private val payloadManager: PayloadDataManager
) : SecondPasswordHandler {
    private var progressDlg: MaterialProgressDialog? = null

    fun validate(ctx: Context, listener: SecondPasswordHandler.ResultListener) {
        if (!hasSecondPasswordSet) {
            listener.onNoSecondPassword()
        } else {
            val passwordField = AppCompatEditText(ctx).apply {
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setHint(R.string.password)
            }

            val view = ctx.getAlertDialogPaddedView(passwordField)
            AlertDialog.Builder(ctx, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.enter_double_encryption_pw)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val secondPassword = passwordField.text.toString()
                    doValidatePassword(secondPassword, listener, view)
                }.setNegativeButton(android.R.string.cancel) { _, _ -> listener.onCancelled() }
                .show()
        }
    }

    @SuppressLint("CheckResult")
    private fun doValidatePassword(
        inputPassword: String,
        listener: SecondPasswordHandler.ResultListener,
        view: FrameLayout
    ) {
        if (inputPassword.isNotEmpty()) {
            showProgressDialog(view.context)
            validateSecondPassword(inputPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate { dismissProgressDialog() }
                .subscribeBy(
                    onNext = { success: Boolean ->
                        if (success) {
                            setValidatePassword(inputPassword)
                            verifiedAt = System.currentTimeMillis()
                            listener.onSecondPasswordValidated(inputPassword)
                        } else {
                            resetValidatedPassword()
                            showErrorSnackbar(view)
                            listener.onCancelled()
                        }
                    },
                    onError = {
                        resetValidatedPassword()
                        showErrorSnackbar(view)
                    }
                )
        } else {
            showErrorSnackbar(view)
            listener.onCancelled()
        }
    }

    private fun showErrorSnackbar(view: View) {
        BlockchainSnackbar.make(
            view, view.context.getString(R.string.double_encryption_password_error), type = SnackbarType.Error
        ).show()
    }

    private fun validateSecondPassword(password: String): Observable<Boolean> {
        return Observable.fromCallable {
            payloadManager.validateSecondPassword(password)
        }
    }

    private fun showProgressDialog(context: Context) {
        dismissProgressDialog()
        progressDlg = MaterialProgressDialog(context).apply {
            setCancelable(false)
            setMessage(R.string.validating_password)
            show()
        }
    }

    private fun dismissProgressDialog() {
        progressDlg?.apply {
            if (isShowing) {
                dismiss()
            }
        }
        progressDlg = null
    }

    override val hasSecondPasswordSet: Boolean
        get() = payloadManager.isDoubleEncrypted

    private var password: String? = null
    private var verifiedAt: Long = 0

    private val isPasswordValid: Boolean
        get() = password != null && System.currentTimeMillis() - verifiedAt < PASSWORD_ACTIVE_TIME_MS

    private fun setValidatePassword(inputPassword: String) {
        password = inputPassword
        verifiedAt = System.currentTimeMillis()
    }

    private fun resetValidatedPassword() {
        password = null
        verifiedAt = 0
    }

    override val verifiedPassword: String?
        get() = when {
            !hasSecondPasswordSet -> ""
            isPasswordValid -> password
            else -> null
        }

    fun secondPassword(ctx: Context): Maybe<String> {
        val subject = MaybeSubject.create<String>()
        val password = verifiedPassword

        when {
            !hasSecondPasswordSet -> subject.onComplete() // empty if no password
            password == null -> validate(
                ctx,
                object : SecondPasswordHandler.ResultListener {
                    override fun onCancelled() {
                        subject.onError(ErrorDialogCancelled())
                    }

                    override fun onNoSecondPassword() {
                        subject.onComplete()
                    }

                    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                        subject.onSuccess(validatedSecondPassword)
                    }
                }
            )
            else -> subject.onSuccess(password)
        }
        return subject
    }

    override fun secondPassword(): Maybe<String> {
        val password = PublishSubject.create<String>()

        return Maybe.defer {
            validate(
                context,
                object : SecondPasswordHandler.ResultListener {
                    override fun onCancelled() {
                        password.onComplete()
                    }

                    override fun onNoSecondPassword() {
                        password.onComplete()
                    }

                    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                        password.onNext(validatedSecondPassword)
                    }
                }
            )
            password.firstElement()
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    companion object {
        private const val PASSWORD_ACTIVE_TIME_MS = 5 * 60 * 1000
    }
}
