package com.simonchung.kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import com.simonchung.kotlin.databinding.ActivityMainBinding
import com.simonchung.kotlin.model.FileModel
import com.simonchung.kotlin.sqlite.Photo
import com.simonchung.kotlin.ui.login.LoginActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths

//debug 時若一直停在 Force Close dialog, 請在 terminal 輸入 adb kill-server -> adb start-server

class MainActivity : AppCompatActivity() {

    private val broadcast = MyReceiver()
    private lateinit var binding: ActivityMainBinding
    private lateinit var v1: String

    private lateinit var list: ArrayList<FileModel>
    private lateinit var adapter: BaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //廣播
        val filter = IntentFilter()
        filter.addAction(packageName)
        registerReceiver(broadcast, filter)

        function("this is function")
        function("this is function 2")

        //Intent
        binding.btnIntent.setOnClickListener {
            val value = binding.et01.text.toString()
            v1 = "$value hello word"
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("info", v1)
            startActivity(intent)
        }

        binding.btnCamera.setOnClickListener {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE))
        }

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnGrid.setOnClickListener {
            val intent = Intent(this, MyGridActivity::class.java)
            startActivity(intent)
        }

        //訊息顯示
        Toast.makeText(this, "this is toast", Toast.LENGTH_LONG).show()
        Snackbar.make(binding.root, "顯示 Snackbar", Snackbar.LENGTH_SHORT).show()

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

        //PreferenceManager 使用前必須先 add dependence
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


        list = getImageList()
        for (i in 0 until list.size) {
            val f: String = list[i].path
            Log.e("aaa", "$f --")
        }

        //thumbnail
        try {
            val uri =
                Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + list[1].id)
            val thumbnail = contentResolver.loadThumbnail(uri, Size(200, 200), null)
            binding.imageView.setImageBitmap(thumbnail)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.gridview.numColumns = 4
        } else binding.gridview.numColumns = 6

        adapter = getAdapter()
        binding.gridview.adapter = adapter

        binding.swipe.setOnRefreshListener {
            list = getImageList()
            adapter.notifyDataSetChanged()
            binding.swipe.isRefreshing = false
        }


    }

    override fun onResume() {
        super.onResume()
        list = getImageList()
        adapter.notifyDataSetChanged()
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
        binding.textView.text = value
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
                model.id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                model.path =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                list.add(model)
            }
            cursor.close()
        }
        val count = list.size
        return list
    }


    private fun getAdapter(): BaseAdapter {
        return object : BaseAdapter() {

            override fun getCount(): Int {
                return list.size
            }

            override fun getItem(position: Int): FileModel {
                return list[position]
            }

            override fun getItemId(i: Int): Long {
                return i.toLong()
            }


            @SuppressLint("ViewHolder")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                if (convertView == null) Log.e("aaa", "aaaa")
                val view: View = layoutInflater.inflate(R.layout.image_picker_images, parent, false)

                try {
                    val model: FileModel = getItem(position)
                    val imageView: ImageView by lazy { view.findViewById(R.id.img_picker_image) }
                    val textView: TextView by lazy { view.findViewById(R.id.img_picker_filename) }
                    val checkBox: CheckBox by lazy { view.findViewById(R.id.img_picker_checkbox) }
                    try {
                        val uri =
                            Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + model.id)
                        val thumbnail = contentResolver.loadThumbnail(uri, Size(200, 200), null)
                        imageView.setImageBitmap(thumbnail)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    val file: File = Paths.get(model.path).toFile()
                    textView.text = file.name
                    checkBox.isChecked = model.selected

                    checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                        model.selected = isChecked
                    }

//                    convertView.setOnClickListener {
//                        //todo
//                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return view
            }
        }
    }


}