// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
}

subprojects {
    val byteBuddyAgent = configurations.create("byteBuddyAgent")
    dependencies {
        byteBuddyAgent("net.bytebuddy:byte-buddy-agent:1.14.17")
    }

    tasks.withType<Test>().configureEach {
        doFirst {
            val agentJar = byteBuddyAgent.singleFile
            jvmArgs(
                "-javaagent:${agentJar.absolutePath}",
                "-XX:+EnableDynamicAgentLoading",
                "-Dnet.bytebuddy.experimental=true",
                "-Djdk.attach.allowAttachSelf=true"
            )
        }
    }
}