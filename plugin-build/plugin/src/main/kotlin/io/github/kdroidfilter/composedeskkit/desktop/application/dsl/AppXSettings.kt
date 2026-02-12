/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package io.github.kdroidfilter.composedeskkit.desktop.application.dsl

@Suppress("UnnecessaryAbstractClass") // Required abstract for Gradle ObjectFactory.newInstance()
abstract class AppXSettings {
    /** Application user model ID */
    var applicationId: String? = null

    /** Publisher display name */
    var publisherDisplayName: String? = null

    /** Display name for the Windows Store */
    var displayName: String? = null

    /** Publisher identity (e.g., "CN=MyCompany") */
    var publisher: String? = null

    /** Identity name (e.g., "MyCompany.MyApp") */
    var identityName: String? = null

    /** Languages supported (e.g., "en-US") */
    var languages: List<String>? = null

    /** Add auto-launch on startup capability. Default: false */
    var addAutoLaunchExtension: Boolean = false
}
