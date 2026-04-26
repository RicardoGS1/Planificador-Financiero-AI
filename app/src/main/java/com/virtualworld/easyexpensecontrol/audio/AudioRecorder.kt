package com.virtualworld.easyexpensecontrol.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Pequeño wrapper sobre [MediaRecorder] para grabar audio en formato AAC (contenedor ADTS),
 * un formato compatible con la API de Gemini.
 *
 * - El archivo de salida se crea bajo `cacheDir/audio_*.aac` y se borra automáticamente
 *   al detener la grabación tras leer su contenido en memoria.
 * - Sólo está pensado para grabaciones cortas (<= 1 minuto) con la voz del usuario.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    /**
     * Inicia una nueva grabación. Si ya había una en curso, la descarta sin enviar nada.
     * Lanza [IOException] si no se pudo preparar el [MediaRecorder].
     */
    @Throws(IOException::class)
    fun start() {
        cancelInternal()
        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.aac")
        val mr = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BIT_RATE_BPS)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        outputFile = file
        startedAtMs = System.currentTimeMillis()
    }

    /**
     * Detiene la grabación y devuelve el contenido del fichero grabado.
     * Devuelve null si no había grabación activa o si no se pudo leer el audio.
     * El archivo temporal se elimina siempre tras este método.
     */
    fun stopAndRead(): ByteArray? {
        val mr = recorder ?: return null
        val file = outputFile
        recorder = null
        outputFile = null
        return try {
            try {
                mr.stop()
            } catch (e: RuntimeException) {
                Log.w(TAG, "MediaRecorder.stop() falló: ${e.message}")
            }
            mr.release()
            file?.takeIf { it.exists() && it.length() > 0 }?.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo audio grabado", e)
            null
        } finally {
            file?.delete()
        }
    }

    /** Cancela la grabación en curso (si la hay) y descarta el archivo. */
    fun cancel() {
        cancelInternal()
    }

    private fun cancelInternal() {
        val mr = recorder
        val file = outputFile
        recorder = null
        outputFile = null
        if (mr != null) {
            try {
                mr.stop()
            } catch (e: RuntimeException) {
                Log.w(TAG, "MediaRecorder.stop() falló al cancelar: ${e.message}")
            }
            mr.release()
        }
        file?.delete()
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE_HZ = 16_000
        private const val BIT_RATE_BPS = 64_000
    }
}
