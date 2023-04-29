package com.simonchung.kotlin

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.simonchung.kotlin.model.FileModel
import com.simonchung.kotlin.sqlite.Photo
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FileOutputStream

//debug 時若一直停在 Force Close dialog, 請在 terminal 輸入 adb kill-server -> adb start-server

class MainActivity : AppCompatActivity() {

    private val et: EditText by lazy { findViewById(R.id.et_01) }
    private val txt: TextView by lazy { findViewById(R.id.textView) }
    private val btn: Button by lazy { findViewById(R.id.button) }
    private val broadcast = MyReceiver()

    private lateinit var v1: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //廣播
        val filter = IntentFilter()
        filter.addAction(packageName)
        registerReceiver(broadcast, filter)

        function("this is function")
        function("this is function 2")

        //Intent
        btn.setOnClickListener {
            val value = et.text.toString()
            v1 = "$value hello word"
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("info", v1)
            startActivity(intent)

            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE))
        }

        //訊息顯示
        Toast.makeText(this, "this is toast", Toast.LENGTH_LONG).show()
        Snackbar.make(findViewById(R.id.textView), "顯示 Snackbar", Snackbar.LENGTH_SHORT).show()

        //執行緒, 不可在Thread內使用Toast會crash, Handler已經deprecate, 可使用廣播與main thread communication
        Thread {
            run {
                for (i in 1..2) {
                    Thread.sleep(1000 * 5)
                    val intent = Intent(packageName)
                    sendBroadcast(intent)
                    Log.e("aaa", "$i ----")
                }
            }
        }.start()

        //權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.CAMERA
                    ), 0
                )
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    ), 0
                )
            }
        }


        //資料庫 sqlite
        try {
            val photo = Photo(this)
            val db = photo.writableDatabase
            val values = ContentValues()
            values.put("file", "000.jpg")
            values.put("path", "c:\\000.jpg")
            db.insert("photo", null, values)
            photo.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        //add dependence into gradle
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        preference.edit().putString("key", "simonchung").apply()

        try {
            val outStream: FileOutputStream = this.openFileOutput("11.txt", Context.MODE_PRIVATE)
            val string = "this is output stream test"
            outStream.write(string.toByteArray())
            outStream.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        //net connection, add dependence into gradle, 不能在 main thread 執行
        Thread {
            run {
                val client = OkHttpClient()
                val request = Request.Builder().url("https://google.com").build()
                try {
                    val response: Response = client.newCall(request).execute()
                    val result: String = response.body().toString()
                    Log.e("aaa", result)
                } catch (e: java.lang.Exception) {
                    Log.e("aaa", e.message.toString())
                }
            }
        }.start()

        val list: ArrayList<FileModel> = getImageList()
        val count = list.size
        Log.e("bbb", "$count---")

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcast)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)
            Toast.makeText(this, "請在設定中開啟權限", Toast.LENGTH_LONG).show()
        else getImageList()
    }

    private fun function(value: String) {
        txt.text = value
    }

    private fun getImageList(): ArrayList<FileModel> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 0)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
            }
        }

        val list: ArrayList<FileModel> = ArrayList()
        var collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }
        val cursor: Cursor? = this.contentResolver.query(
            collection,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            ),
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " ASC"
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val model = FileModel()
                model.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)))
                model.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)))
                list.add(model)
            }
            cursor.close()
        }
        val count = list.size
        Log.e("aaa", "$count---")
        return list
    }


}