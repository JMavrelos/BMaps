package gr.blackswamp.bmaps.utils

import android.os.AsyncTask
import android.os.Environment

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

import timber.log.Timber

internal class StoreHistoryTask(private val navigation: MapboxNavigation, private val filename: String) : AsyncTask<Void, Void, Void>() {

    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    override fun doInBackground(vararg paramsUnused: Void): Void? {
        if (isExternalStorageWritable) {
            val history = navigation.retrieveHistory()
            if (!history.contentEquals(EMPTY_HISTORY)) {
                val pathToExternalStorage = Environment.getExternalStorageDirectory()
                val appDirectory = File(pathToExternalStorage.absolutePath + DRIVES_FOLDER)
                appDirectory.mkdirs()
                val saveFilePath = File(appDirectory, filename)
                write(history, saveFilePath)
            }
        }
        return null
    }

    private fun write(history: String, saveFilePath: File) {
        try {
            val fos = FileOutputStream(saveFilePath)
            val outDataWriter = OutputStreamWriter(fos)
            outDataWriter.write(history)
            outDataWriter.close()
            fos.flush()
            fos.close()
        } catch (exception: Exception) {
            Timber.e(exception)
        }

    }

    companion object {
        private val EMPTY_HISTORY = "{}"
        private val DRIVES_FOLDER = "/drives"
    }
}
