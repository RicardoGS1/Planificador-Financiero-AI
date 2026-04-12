package com.virtualworld.easyexpensecontrol.ui.contracts

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/**
 * Equivalente a [androidx.activity.result.contract.ActivityResultContracts.TakePicture] con
 * [Intent.FLAG_GRANT_READ_URI_PERMISSION] y [Intent.FLAG_GRANT_WRITE_URI_PERMISSION] y
 * [ClipData] explícita, como exige el comportamiento en Android 18+ (evita concesiones URI implícitas).
 */
class TakePictureWithUriGrants : ActivityResultContract<Uri, Boolean>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input)
            addFlags(flags)
            clipData = ClipData.newUri(context.contentResolver, "output", input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}
