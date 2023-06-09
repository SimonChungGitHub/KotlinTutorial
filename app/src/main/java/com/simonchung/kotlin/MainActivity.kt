package com.simonchung.kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
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
import com.google.android.material.snackbar.Snackbar
import com.simonchung.kotlin.broadcast.MyReceiver
import com.simonchung.kotlin.databinding.ActivityMainBinding
import com.simonchung.kotlin.model.FileModel
import com.simonchung.kotlin.sqlite.Photo
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

//debug 時若一直停在 Force Close dialog, 請在 terminal 輸入 adb kill-server -> adb start-server

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var v1: String
    private val broadcast = MyReceiver()
    private lateinit var dialog: AlertDialog

    private lateinit var list: ArrayList<FileModel>
    private lateinit var adapter: BaseAdapter

    private val uploadReceive: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (dialog.isShowing) dialog.dismiss()
            list = getImageList()
            adapter.notifyDataSetChanged()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //建立 dialog
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPaddingRelative(20, 50, 20, 0)
        val builder = AlertDialog.Builder(this)
        builder.setView(linearLayout).setCancelable(false)
        dialog = builder.create()

        //廣播
        val filter = IntentFilter()
        filter.addAction(packageName)
        registerReceiver(broadcast, filter)
        registerReceiver(uploadReceive, filter)

        list = getImageList()
        for (i in 0 until list.size) {
            val f: String = list[i].path
            Log.e("aaa", "$f --")
        }

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

        binding.btnUpload.setOnClickListener {
            val uploadList = list.stream().filter { o -> o.selected }.collect(
                Collectors.toList()
            ) as ArrayList<FileModel?>

            val progressbar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
            progressbar.progress = 0
            progressbar.max = uploadList.size

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            linearLayout.removeAllViews()
            linearLayout.addView(progressbar)
            dialog.show()

            Thread {
                run {
                    for (i in 0 until uploadList.size) {
                        progressbar.incrementProgressBy(1)
                        val fileModel = uploadList[i] as FileModel
                        upload(fileModel)
                    }
                    sendBroadcast(Intent(packageName))
                }
            }.start()
        }


        //訊息顯示
//        Toast.makeText(this, "this is toast", Toast.LENGTH_LONG).show()
//        Snackbar.make(binding.root, "顯示 Snackbar", Snackbar.LENGTH_SHORT).show()

        //執行緒, 不可在Thread內使用Toast會crash, Handler已經deprecate, 可使用廣播與main thread communication
        Thread {
            run {
                for (i in 1..2) {
                    Thread.sleep(1000)
//                    val intent = Intent(packageName)
//                    sendBroadcast(intent)
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

        //FileOutputStream
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

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.gridview.numColumns = 4
        } else binding.gridview.numColumns = 6

        adapter = getAdapter()
        binding.gridview.adapter = adapter

        //swipe
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
        unregisterReceiver(uploadReceive)
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
                    val thumbnail = thumbnail(model.id)
                    imageView.setImageBitmap(thumbnail)


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

    private fun thumbnail(id: Long): Bitmap {
        val uri =
            Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + id)
        return contentResolver.loadThumbnail(uri, Size(200, 200), null)
    }

    //請在 Thread 中執行
    private fun upload(model: FileModel) {
        val file = Paths.get(model.path).toFile()
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("dept", "temp")
            .addFormDataPart(
                "image", file.name,
                RequestBody.create(MediaType.parse("image/jpeg"), file)
            )
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()

        val url = "http://192.168.0.238/okhttp/api/values/FileUpload"
        val request = Request.Builder()
            .header("Content-Type", "multipart/form-data")
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Snackbar.make(binding.root, "code: ${response.code()}", Snackbar.LENGTH_SHORT)
                    .show()
//                sendBroadcast(Intent(packageName))
            }
        } catch (e: IOException) {
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_SHORT)
                .show()
        }
    }


}