package com.goreecloud.gifs

import androidx.core.content.FileProvider

/**
 * AndroidX recommends an app-specific subclass for compatibility across devices.
 * Paths and temporary read grants remain constrained by res/xml/file_paths.xml.
 */
class GoreeCloudFileProvider : FileProvider()
