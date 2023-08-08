@file:Suppress("unused")

object Versions {

    // Release info
    const val minSdk = 26

    const val targetSdk = 33
    const val compileSdk = 33

    const val versionCode = 26730
    const val versionName = "202308.2.0"

    const val buildTools = "30.0.3"

    // Build tools and languages
    const val androidPlugin = "8.0.1"
    const val kotlin = "1.7.0"

    // Coroutines
    const val coroutines = "1.6.4"

    // Kotlin Immutable Collections
    const val immutableCollections = "0.3.5"

    const val googleServicesPlugin = "4.3.3"
    const val ktlint = "0.45.0"
    const val detekt = "1.20.0"
    const val kotlinJvmTarget = "1.8"
    const val javaCompatibilityVersion = 1.8

    // Androidx Libraries
    const val appCompat = "1.3.1"
    const val recyclerview = "1.2.1"
    const val cardview = "1.0.0"
    const val gridlayout = "1.0.0"
    const val design = "1.4.0"
    const val preference = "1.1.0"
    const val dynamicanimation = "1.0.0"
    const val annotations = "1.2.0"
    const val constraintLayout = "2.0.4"
    const val multidex = "2.0.1"
    const val desugaring = "1.1.5"
    const val installReferrer = "2.2"
    const val navigation = "2.5.0"
    const val lifecycle = "2.4.1"
    const val camera = "1.0.0-rc04"
    const val cameraView = "1.0.0-alpha23"
    const val customTabs = "1.5.0"

    // Support Libraries
    const val googleServices = "17.1.0"
    const val googleServicesAuth = "19.0.0"
    const val googleServicesPlaces = "2.7.0"
    const val googleServicesReCaptcha = "16.0.0"
    const val googleServicesWallet = "19.0.1"
    const val googleServicesCoroutines = "1.6.4"
    const val googleServicesTapAndPay = "18.2.0"
    const val googleServicesBasement = "18.1.0"
    const val googleServicesTasks = "18.0.2"
    const val googlePlayCore = "1.8.2"
    const val firebaseBom = "31.0.1"
    const val supportTesting = "1.0.2"
    const val biometrics = "1.1.0-rc01"
    const val adsIdentifier = "17.0.0"

    // Networking, RxJava
    const val chucker = "3.5.2"
    const val retrofit = "2.9.0"
    const val okHttp = "4.10.0"
    const val gson = "2.8.5"
    const val kotlinJson = "1.3.2"
    const val kotlinJsonConverter = "0.8.0"

    // SqlDelight
    const val sqlDelight = "1.5.0"

    const val koin = "3.3.2"
    const val koinAndroid = "3.3.1"
    const val koinCompose = "3.3.0"
    const val rxJava = "3.0.7"
    const val rxKotlin = "3.0.1"
    const val rxAndroid = "3.0.0"
    const val rxBinding = "4.0.0"
    const val rxReplayShare = "3.0.0"
    const val rxRelay = "3.0.0"
    const val glide = "4.14.2"

    // Utils, BTC, Ethereum
    const val bitcoinj = "0.16"
    const val web3j = "4.8.7-android"
    const val spongycastle = "1.54.0.0"
    const val jjwt = "0.9.0"
    const val lambdaWorks = "1.0.0"
    const val libPhoneNumber = "8.12.41"
    const val commonsCodec = "1.3" // Keep at 1.3 to match Android
    const val commonsLang = "3.4"
    const val commonsCli = "1.3"
    const val commonsIo = "2.6"
    const val walletConnect = "1.5.4"
    const val urlBuilder = "2.0.9"
    const val yearclass = "2.0.0"
    const val protobuf = "3.18.0"
    const val findbugs = "3.0.2"
    const val guava = "28.0-android"
    const val dexter = "6.2.1"

    // Custom Views
    const val charts = "3.1.0"
    const val circleIndicator = "2.1.6"
    const val countryPicker = "2.0.4"

    // Compose
    const val composeCompiler = "1.2.0"
    const val compose = "1.4.2"
    const val composeShowkase = "1.1.1"
    const val coil = "2.2.2"
    const val landscapist = "2.1.3"
    const val composeActivity = "1.5.0"
    const val constraintLayoutCompose = "1.0.1"
    const val composeRx = "1.2.0"
    const val composeNavigation = "2.5.1"
    const val composeFlowLayout = "0.25.0"
    const val composeLifecycle = "2.6.0-alpha03"
    const val composePoolingContainer = "1.0.0"

    // Accompanist
    const val systemUi = "0.25.0"
    const val navigationMaterial = "0.29.1-alpha"
    const val swipeRefresh = "0.29.1-alpha"
    const val pager = "0.25.0"
    const val pagerIndicators = "0.25.0"
    const val drawablePainter = "0.25.0"
    const val permissions = "0.31.5-beta"

    // zxing 3.4.0 crashes with:
    //      "java.lang.NoSuchMethodError. No interface method sort(Ljava/util/Comparator;)V in class Ljava/util/List;"
    // List.sort() is not available on Android SDK < 24 so DO NOT UPGRADE until project target min is 24
    const val zxing = "3.5.1"
    const val materialDatePicker = "3.6.4"
    const val sparkline = "1.2.0"

    // Third Party SDKs
    const val veriff = "6.8.0"
    const val sift = "0.11.1"
    const val cardForm = "4.2.0"
    const val xlmSunriver = "0.21.1"
    const val lottieVersion = "5.2.0"
    const val zendeskChatVersion = "3.3.4"
    const val zendeskMessagingVersion = "5.2.3"
    const val checkoutComVersion = "3.0.1"
    const val stripeVersion = "20.3.0"
    const val intercomVersion = "15.0.0"
    const val plaidLinkVersion = "3.9.0"
    const val exoPlayerVersion = "2.18.1"
    const val vgsVersion = "1.7.1"

    /**
     * [composeMarkdownVersion]
     * If you are updating this dependency, please make sure to post in this ticket:
     * https://blockchain.atlassian.net/browse/SEC-313
     * So security can re-scan for vulnerabilities, as this is not a commonly used library
     */
    const val composeMarkdownVersion = "0.3.0"

    // Logging
    const val timber = "4.7.1"
    const val slf4j = "1.7.20"
    const val firebaseCrashlyticsPlugin = "2.9.2"
    const val embraceIO = "5.0.4"

    // Debugging
    const val stetho = "1.5.1"

    // Testing
    const val mockito = "3.12.4"
    const val mockitoKotlin = "2.0.0"
    const val kluent = "1.66"
    const val coroutinesTest = "1.6.0"
    const val coreTesting = "2.1.0"
    const val turbine = "0.8.0"
    const val mockk = "1.12.2"
    const val hamcrestJunit = "2.0.0.0"
    const val junit = "4.13.2"
    const val robolectric = "4.10.3"

    const val json = "20140107"
    const val espresso = "3.2.0"
    const val androidxTesting = "1.3.0"
}

object Libraries {

    // Build tools and languages
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidPlugin}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinSerializationPlugin = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val androidCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val rxjava3Coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-rx3:${Versions.coroutines}"
    const val kotlinImmutableCollections =
        "org.jetbrains.kotlinx:kotlinx-collections-immutable:${Versions.immutableCollections}"
    const val googleServicesPlugin = "com.google.gms:google-services:${Versions.googleServicesPlugin}"
    const val ktlint = "com.pinterest:ktlint:${Versions.ktlint}"
    const val ktlintCore = "com.pinterest.ktlint:ktlint-core:${Versions.ktlint}"
    const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${Versions.detekt}"
    const val detektFormat = "io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.detekt}"

    // Support Libraries
    const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
    const val recyclerView = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"
    const val cardView = "androidx.cardview:cardview:${Versions.cardview}"
    const val gridLayout = "androidx.gridlayout:gridlayout:${Versions.gridlayout}"
    const val design = "com.google.android.material:material:${Versions.design}"
    const val v14 = "androidx.preference:preference:${Versions.preference}"
    const val dynamicAnims = "androidx.dynamicanimation:dynamicanimation:${Versions.dynamicanimation}"
    const val androidXAnnotations = "androidx.annotation:annotation:${Versions.annotations}"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    const val multidex = "androidx.multidex:multidex:${Versions.multidex}"
    const val desugaring = "com.android.tools:desugar_jdk_libs:${Versions.desugaring}"
    const val installReferrer = "com.android.installreferrer:installreferrer:${Versions.installReferrer}"
    const val navigationControllerCore = "androidx.navigation:navigation-ui:${Versions.navigation}"
    const val navigationControllerFragments = "androidx.navigation:navigation-fragment:${Versions.navigation}"
    const val navigationControllerSafeArgsPlugin =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
    const val navigationControllerCoreKTX = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"

    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycle}"
    const val lifecycleViewModelExtensions = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
    const val camera = "androidx.camera:camera-camera2:${Versions.camera}"
    const val cameraView = "androidx.camera:camera-view:${Versions.cameraView}"
    const val cameraLifecycle = "androidx.camera:camera-lifecycle:${Versions.camera}"
    const val customTabs = "androidx.browser:browser:${Versions.customTabs}"

    // Google & Firebase
    const val firebaseBom = "com.google.firebase:firebase-bom:${Versions.firebaseBom}"
    const val firebaseConfig = "com.google.firebase:firebase-config"
    const val firebaseMessaging = "com.google.firebase:firebase-messaging"
    const val firebaseDynamicLink = "com.google.firebase:firebase-dynamic-links"
    const val googlePlayServicesBase =
        "com.google.android.gms:play-services-base:${Versions.googleServices}"
    const val googlePlayServicesBasement =
        "com.google.android.gms:play-services-basement:${Versions.googleServicesBasement}"
    const val googlePlayServicesTasks =
        "com.google.android.gms:play-services-tasks:${Versions.googleServicesTasks}"
    const val googlePlayServicesAuth =
        "com.google.android.gms:play-services-auth:${Versions.googleServicesAuth}"
    const val googleServicesReCaptcha =
        "com.google.android.gms:play-services-recaptcha:${Versions.googleServicesReCaptcha}"
    const val googleServicesWallet =
        "com.google.android.gms:play-services-wallet:${Versions.googleServicesWallet}"
    const val googlePlayCore = "com.google.android.play:core:${Versions.googlePlayCore}"
    const val googlePlayServicesCoroutines =
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Versions.googleServicesCoroutines}"
    const val googlePlayServicesTapAndPay =
        "com.google.android.gms:play-services-tapandpay:${Versions.googleServicesTapAndPay}"

    const val biometricsApi = "androidx.biometric:biometric:${Versions.biometrics}"
    const val adsIdentifier = "com.google.android.gms:play-services-ads-identifier:${Versions.adsIdentifier}"

    // Networking, RxJava
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val retrofitRxJavaAdapter = "com.squareup.retrofit2:adapter-rxjava3:${Versions.retrofit}"
    const val retrofitKotlinJsonConverter =
        "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:${Versions.kotlinJsonConverter}"
    const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
    const val okHttpSse = "com.squareup.okhttp3:okhttp-sse:${Versions.okHttp}"
    const val okHttpInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"
    const val gson = "com.google.code.gson:gson:${Versions.gson}"
    const val kotlinJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinJson}"

    // SqlDelight
    const val sqlDelight = "com.squareup.sqldelight:android-driver:${Versions.sqlDelight}"
    const val sqlDelightPlugin = "com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}"
    const val rxSqlDelight = "com.squareup.sqldelight:rxjava3-extensions:${Versions.sqlDelight}"
    const val coroutinesSqlDelight = "com.squareup.sqldelight:coroutines-extensions:${Versions.sqlDelight}"

    const val koin = "io.insert-koin:koin-core:${Versions.koin}"
    const val koinAndroid = "io.insert-koin:koin-android:${Versions.koinAndroid}"
    const val koinTest = "io.insert-koin:koin-test:${Versions.koin}"
    const val koinTestJunit = "io.insert-koin:koin-test-junit4:${Versions.koin}"
    const val koinCompose = "io.insert-koin:koin-androidx-compose:${Versions.koinCompose}"
    const val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
    const val glideAnnotations = "com.github.bumptech.glide:compiler:${Versions.glide}"
    const val chuckerDebug = "com.github.chuckerteam.chucker:library:${Versions.chucker}"
    const val chuckerRelease = "com.github.chuckerteam.chucker:library-no-op:${Versions.chucker}"

    const val rxJava = "io.reactivex.rxjava3:rxjava:${Versions.rxJava}"
    const val rxKotlin = "io.reactivex.rxjava3:rxkotlin:${Versions.rxKotlin}"
    const val rxAndroid = "io.reactivex.rxjava3:rxandroid:${Versions.rxAndroid}"
    const val rxBinding = "com.jakewharton.rxbinding4:rxbinding:${Versions.rxBinding}"
    const val rxBindingCore = "com.jakewharton.rxbinding4:rxbinding-core:${Versions.rxBinding}"
    const val rxBindingCompat = "com.jakewharton.rxbinding4:rxbinding-appcompat:${Versions.rxBinding}"
    const val rxReplayShareKotlin = "com.jakewharton.rx3:replaying-share-kotlin:${Versions.rxReplayShare}"
    const val rxRelay = "com.jakewharton.rxrelay3:rxrelay:${Versions.rxRelay}"

    // Utils, BTC, Ethereum
    const val bitcoinj = "org.bitcoinj:bitcoinj-core:${Versions.bitcoinj}"
    const val web3j = "org.web3j:core:${Versions.web3j}"
    const val walletConnect = "com.github.TrustWallet:wallet-connect-kotlin:${Versions.walletConnect}"
    const val walletConnectV2Web3Wallet = "com.walletconnect:web3wallet:1.10.0"
    const val walletConnectV2AndroidCore = "com.walletconnect:android-core:1.17.0"
    const val spongyCastle = "com.madgag.spongycastle:prov:${Versions.spongycastle}"
    const val jjwt = "io.jsonwebtoken:jjwt:${Versions.jjwt}"
    const val lambdaWorks = "com.lambdaworks:codec:${Versions.lambdaWorks}"
    const val commonsCodec = "commons-codec:commons-codec:${Versions.commonsCodec}"
    const val commonsLang = "org.apache.commons:commons-lang3:${Versions.commonsLang}"
    const val commonsCli = "commons-cli:commons-cli:${Versions.commonsCli}"
    const val commonsIo = "commons-io:commons-io:${Versions.commonsIo}"
    const val urlBuilder = "io.mikael:urlbuilder:${Versions.urlBuilder}"
    const val yearclass = "com.facebook.device.yearclass:yearclass:${Versions.yearclass}"
    const val protobuf = "com.google.protobuf:protobuf-javalite:${Versions.protobuf}"
    const val findbugs = "com.google.code.findbugs:jsr305:${Versions.findbugs}"
    const val guava = "com.google.guava:guava:${Versions.guava}"
    const val dexter = "com.karumi:dexter:${Versions.dexter}"
    const val libPhoneNumber = "io.michaelrocks:libphonenumber-android:${Versions.libPhoneNumber}"

    // Custom Views
    const val charts = "com.github.PhilJay:MPAndroidChart:v${Versions.charts}"
    const val circleIndicator = "me.relex:circleindicator:${Versions.circleIndicator}@aar"
    const val countryPicker = "com.github.mukeshsolanki:country-picker-android:${Versions.countryPicker}"
    const val zxing = "com.google.zxing:core:${Versions.zxing}"
    const val materialDatePicker = "com.wdullaer:materialdatetimepicker:${Versions.materialDatePicker}"
    const val sparkline = "com.robinhood.spark:spark:${Versions.sparkline}"

    // Compose versions for Showkase app
    const val composeFoundationShowkase = "androidx.compose.foundation:foundation:${Versions.composeShowkase}"
    const val composeUiShowkase = "androidx.compose.ui:ui:${Versions.composeShowkase}"
    const val composeMaterialShowkase = "androidx.compose.material:material:${Versions.composeShowkase}"
    const val composePreviewShowkase = "androidx.compose.ui:ui-tooling-preview:${Versions.composeShowkase}"
    const val composeUiToolingShowkase = "androidx.compose.ui:ui-tooling:${Versions.composeShowkase}"
    const val composeUiTestShowkase = "androidx.compose.ui:ui-test-junit4:${Versions.composeShowkase}"

    // Compose
    const val composeFoundation = "androidx.compose.foundation:foundation:${Versions.compose}"
    const val composeUi = "androidx.compose.ui:ui:${Versions.compose}"
    const val composeUiUtil = "androidx.compose.ui:ui-util:${Versions.compose}"
    const val composeMaterial = "androidx.compose.material:material:${Versions.compose}"
    const val composePreview = "androidx.compose.ui:ui-tooling-preview:${Versions.compose}"
    const val composeUiTooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
    const val composeUiPoolingContainer =
        "androidx.customview:customview-poolingcontainer:${Versions.composePoolingContainer}"
    const val composeActivity = "androidx.activity:activity-compose:${Versions.composeActivity}"
    const val composeUiTest = "androidx.compose.ui:ui-test-junit4:${Versions.compose}"
    const val composelifecycle = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.composeLifecycle}"
    const val composeCoil = "io.coil-kt:coil-compose:${Versions.coil}"
    const val composeCoilSvg = "io.coil-kt:coil-svg:${Versions.coil}"
    const val composeCoilGif = "io.coil-kt:coil-gif:${Versions.coil}"
    const val composeLandScapist = "com.github.skydoves:landscapist-glide:${Versions.landscapist}"
    const val composeLottie = "com.airbnb.android:lottie-compose:${Versions.lottieVersion}"
    const val constraintLayoutCompose =
        "androidx.constraintlayout:constraintlayout-compose:${Versions.constraintLayoutCompose}"
    const val composeRx = "androidx.compose.runtime:runtime-rxjava3:${Versions.composeRx}"

    // Accompanist
    const val accompanistSystemUi = "com.google.accompanist:accompanist-systemuicontroller:${Versions.systemUi}"
    const val accompanistNavigationMaterial =
        "com.google.accompanist:accompanist-navigation-material:${Versions.navigationMaterial}"
    const val composeNavigation = "androidx.navigation:navigation-compose:${Versions.composeNavigation}"
    const val accompanistFlowLayout = "com.google.accompanist:accompanist-flowlayout:${Versions.composeFlowLayout}"
    const val accompanistSwipeRefresh = "com.google.accompanist:accompanist-swiperefresh:${Versions.swipeRefresh}"
    const val accompanistPager = "com.google.accompanist:accompanist-pager:${Versions.pager}"
    const val accompanistPagerIndicators =
        "com.google.accompanist:accompanist-pager-indicators:${Versions.pagerIndicators}"
    const val accompanistDrawablePainter =
        "com.google.accompanist:accompanist-drawablepainter:${Versions.drawablePainter}"
    const val accompanistPermissions =
        "com.google.accompanist:accompanist-permissions:${Versions.permissions}"

    // Third Party SDKs
    const val veriff = "com.veriff:veriff-library:${Versions.veriff}"
    const val sift = "com.siftscience:sift-android:${Versions.sift}"
    const val cardForm = "com.braintreepayments:card-form:${Versions.cardForm}"
    const val sunriver = "com.github.stellar:java-stellar-sdk:${Versions.xlmSunriver}"
    const val lottie = "com.airbnb.android:lottie:${Versions.lottieVersion}"
    const val zendeskChat = "com.zendesk:chat:${Versions.zendeskChatVersion}"
    const val zendeskMessaging = "com.zendesk:messaging:${Versions.zendeskMessagingVersion}"
    const val checkoutCom = "com.github.checkout:frames-android:${Versions.checkoutComVersion}"
    const val stripe = "com.stripe:stripe-android:${Versions.stripeVersion}"
    const val intercom = "io.intercom.android:intercom-sdk-base:${Versions.intercomVersion}"
    const val plaidLink = "com.plaid.link:sdk-core:${Versions.plaidLinkVersion}"
    const val exoPlayer = "com.google.android.exoplayer:exoplayer:${Versions.exoPlayerVersion}"
    const val composeMarkdown = "com.github.jeziellago:compose-markdown:${Versions.composeMarkdownVersion}"
    const val vgs = "com.verygoodsecurity:vgscollect:${Versions.vgsVersion}"

    // Logging
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    const val slf4j = "org.slf4j:slf4j-simple:${Versions.slf4j}"
    const val slf4jNoOp = "org.slf4j:slf4j-nop:${Versions.slf4j}"
    const val firebaseCrashlytics = "com.google.firebase:firebase-crashlytics"
    const val firebaseCrashlyticsPlugin =
        "com.google.firebase:firebase-crashlytics-gradle:${Versions.firebaseCrashlyticsPlugin}"
    const val firebaseAnalytics = "com.google.firebase:firebase-analytics"

    // Debugging
    const val stetho = "com.facebook.stetho:stetho:${Versions.stetho}"
    const val stethoOkHttp = "com.facebook.stetho:stetho-okhttp3:${Versions.stetho}"

    // Testing
    const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutinesTest}"
    const val coreTesting = "androidx.arch.core:core-testing:${Versions.coreTesting}"
    const val turbine = "app.cash.turbine:turbine:${Versions.turbine}"
    const val mockk = "io.mockk:mockk:${Versions.mockk}"
    const val hamcrestJunit = "org.hamcrest:hamcrest-junit:${Versions.hamcrestJunit}"
    const val junit = "junit:junit:${Versions.junit}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val json = "org.json:json:${Versions.json}"
    const val testRules = "androidx.test:rules:${Versions.androidxTesting}"
    const val testRunner = "androidx.test:runner:${Versions.androidxTesting}"
    const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    const val retrofitMock = "com.squareup.retrofit2:retrofit-mock:${Versions.retrofit}"
    const val okHttpMock = "com.squareup.okhttp3:mockwebserver:${Versions.okHttp}"
}
