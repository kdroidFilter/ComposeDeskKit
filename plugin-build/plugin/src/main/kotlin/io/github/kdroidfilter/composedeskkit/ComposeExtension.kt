/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package io.github.kdroidfilter.composedeskkit

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import javax.inject.Inject

abstract class ComposeExtension
    @Inject
    constructor(
        project: Project,
    ) : ExtensionAware {
        val dependencies = ComposePlugin.Dependencies(project)
    }
