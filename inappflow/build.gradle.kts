plugins {
    alias(libs.plugins.android.library)

    `maven-publish`
}

android {
    namespace = "com.elytelabs.inappflow"
    compileSdk = 37

    defaultConfig {
        minSdk = 25

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.review.ktx)
    implementation(libs.app.update.ktx)
}

// Maven publishing configuration
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.elytelabs.inappflow"
                artifactId = "inappflow"
                version = "1.2.0"
                from(components["release"])

                pom {
                    name.set("InAppFlow")
                    description.set("InApp Review & Update Library")
                    url.set("https://github.com/elytelabs/inappflow")

                    licenses {
                        license {
                            name.set("Apache 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}