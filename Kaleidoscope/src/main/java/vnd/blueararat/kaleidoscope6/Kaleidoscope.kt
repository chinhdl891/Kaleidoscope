package vnd.blueararat.kaleidoscope6

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.CursorIndexOutOfBoundsException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.SensorManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class Kaleidoscope : AppCompatActivity() {
    var preferences: SharedPreferences? = null
    private var mNumberOfMirrors = 0
    private var mBitmap // sNewBitmap, sViewBitmap, mBitmap, sExportBitmap;
            : Bitmap? = null
    private var imageUri: Uri? = null
    private var mK: KView? = null
    private var sStringUri = ""
    private var mFrame: FrameLayout? = null
    private var mMenu: Menu? = null
    private var mGLSurfaceView: GLSurfaceView? = null
    private var mK3DRenderer: K3DRenderer? = null
    private var bCameraInMenu = false
    private var mOverlayView: View? = null
    val width: Int
        get() = mFrame?.width ?: 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState ?: Bundle())
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        sStringUri = preferences!!.getString(KEY_IMAGE_URI, "")!!
        bCameraInMenu = preferences!!.getBoolean(KEY_CAMERA_IN_MENU, true)
        val options = BitmapFactory.Options()
        options.inScaled = false
        loadBitmap(options, null)
        mOverlayView = View(this)
        mOverlayView!!.setBackgroundColor(Color.BLACK)
        mK = KView(this, mBitmap)
        mNumberOfMirrors = mK!!.numberOfMirrors
        setContentView(R.layout.main)
        mFrame = findViewById(R.id.frame)
        mFrame?.addView(mK)
        toggleHardwareAcceleration(preferences!!.getBoolean(KEY_HARDWARE_ACCEL, true))
        // setContentView(mK);

        mK!!.viewTreeObserver.addOnDrawListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mK!!.drawPoints(display?.width?: 1080, display?.height?: 1920)
            }else{
                mK!!.drawPoints(getScreenSize().x, getScreenSize().y)

            }
        }

    }

    fun getScreenSize(): Point {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize
    }

    override fun onResume() {
        super.onResume()
        if (mGLSurfaceView != null) {
            mGLSurfaceView!!.onResume()
            Renew().execute()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mGLSurfaceView != null) {
            mGLSurfaceView!!.onPause()
            mK3DRenderer!!.stop()
        }
    }

    override fun onStart() {
        super.onStart()
        if (use3D) {
            Renew().execute()
        }
    }

    internal inner class Renew : AsyncTask<Void?, Void?, Void?>() {

        override fun onPostExecute(result: Void?) {
            if (mGLSurfaceView == null) {
                toggle3D(use3D)
            } else {
                mK!!.updateTexture()
                mK3DRenderer!!.start()
            }
        }

        override fun doInBackground(vararg params: Void?): Void? {
            SystemClock.sleep(5)
            return null
        }
    }

    // @Override
    // protected void onPostCreate(Bundle savedInstanceState) {
    // super.onPostCreate(savedInstanceState);
    //
    // }
    private fun loadBitmap(options: BitmapFactory.Options, uri: Uri?) {
        var opts: BitmapFactory.Options? = BitmapFactory.Options()
        opts!!.inScaled = false
        opts.inJustDecodeBounds = true
        var input: InputStream
        if (uri == null) {
            BitmapFactory.decodeResource(resources, R.drawable.img1, opts)
        } else {
            try {
                input = this.contentResolver.openInputStream(uri)!!
                BitmapFactory.decodeStream(input, null, opts)
                input.close()
            } catch (e: FileNotFoundException) {
                opts = null
                loadBitmap(options, null)
                return
            } catch (e: IOException) {
                opts = null
                loadBitmap(options, null)
                return
            }
            if (opts.outWidth == -1 || opts.outHeight == -1) {
                opts = null
                loadBitmap(options, null)
                return
            }
        }
        var b = Memory.checkBitmapFitsInMemory(opts)
        if (b) {
            if (uri == null) {
                mBitmap = BitmapFactory.decodeResource(
                    resources, R.drawable.img1, options
                )
                return
            } else {
                try {
                    input = this.contentResolver.openInputStream(uri)!!
                    mBitmap = BitmapFactory.decodeStream(input, null, options)
                    input.close()
                    mK!!.redraw(mBitmap)
                    // mBitmap =
                    // MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                    // uri);
                    return
                } catch (e: FileNotFoundException) {
                    opts = null
                    loadBitmap(options, null)
                    return
                } catch (e: IOException) {
                    opts = null
                    loadBitmap(options, null)
                    return
                } catch (e: OutOfMemoryError) {
                    b = false
                }
            }
        }
        if (!b) {
            // opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inSampleSize = 1
            while (!b) {
                opts.inSampleSize += 1
                if (uri == null) {
                    BitmapFactory.decodeResource(
                        resources, R.drawable.img1, opts
                    )
                } else {
                    try {
                        input = this.contentResolver.openInputStream(uri)!!
                        BitmapFactory.decodeStream(input, null, opts)
                        input.close()
                    } catch (e: FileNotFoundException) {
                    } catch (e: IOException) {
                    }
                }
                b = Memory.checkBitmapFitsInMemory(opts)
            }
            opts.inJustDecodeBounds = false
            if (uri == null) {
                mBitmap = BitmapFactory.decodeResource(
                    resources, R.drawable.img1, opts
                )
            } else {
                try {
                    input = this.contentResolver.openInputStream(uri)!!
                    mBitmap = BitmapFactory.decodeStream(input, null, opts)
                    input.close()
                } catch (e: FileNotFoundException) {
                } catch (e: IOException) {
                }
            }
            Toast.makeText(
                this,
                getString(R.string.picture_was_too_large) + " " + opts.inSampleSize + " " + getString(
                    R.string.times
                ),
                Toast.LENGTH_LONG
            ).show()
        }
        System.gc()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBitmap!!.recycle()
        mK!!.destroy()
        mK = null
        if (use3D) {
            mK3DRenderer!!.stop()
            mGLSurfaceView = null
        }
        System.gc()
        System.gc()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate our menu which can gather user input for switching camera
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        mMenu = menu
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val k3d = menu.findItem(R.id.K3D)
        if (use3D) {
            k3d.setIcon(R.drawable.ic_menu_2d)
            k3d.title = "2D"
        } else {
            k3d.setIcon(R.drawable.ic_menu_3d)
            k3d.title = "3D"
        }
        val camera = menu.findItem(R.id.camera)
        if (camera != null) {
            camera.isVisible = bCameraInMenu
            camera.isEnabled = bCameraInMenu
        }
        mMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, Prefs::class.java)
                startActivityForResult(intent, CHANGE_NUMBER_OF_MIRRORS)
                return true
            }

            R.id.open -> {
                val intent2 = Intent()
                intent2.type = "image/*"
                intent2.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(
                        intent2, getString(R.string.open_picture)
                    ), OPEN_PICTURE
                )
                return true
            }

            R.id.camera -> {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.CAMERA), CAN_USE_CAMERA_PERMISSION_RESULT
                    )
                } else {
                    val intent3 = Intent(this, KCamera::class.java)
                    finish()
                    startActivity(intent3)
                }
                return true
            }

            R.id.export -> {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        CAN_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT
                    )
                } else {
                    export()
                }
                return true
            }

            R.id.K3D -> {
                toggle3D(!use3D.also { use3D = it })
                return true
            }

            R.id.exit -> {
                finish()
                return true
            }
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAN_USE_CAMERA_PERMISSION_RESULT -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    val intent3 = Intent(this, KCamera::class.java)
                    finish()
                    startActivity(intent3)
                }
            }

            CAN_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    export()
                }
                return
            }
        }
    }

    private fun export() {
        if (!use3D) {
            Export().execute()
        } else {
            mK3DRenderer!!.setShouldExport(true)
        }
    }

    private fun toggle3D(use: Boolean) {
        if (use) {
            val sm = getSystemService(SENSOR_SERVICE) as SensorManager
            mK3DRenderer = K3DRenderer(this, sm)
            mGLSurfaceView = GLSurfaceView(this)
            mGLSurfaceView!!.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            mGLSurfaceView!!.setRenderer(mK3DRenderer)
            mGLSurfaceView!!.holder.setFormat(PixelFormat.TRANSLUCENT)
            mGLSurfaceView!!.setZOrderOnTop(true)
            val flp = FrameLayout.LayoutParams(
                mFrame!!.layoutParams
            )
            val margin = (mFrame!!.height - mFrame!!.width) / 2
            flp.gravity = Gravity.CENTER
            flp.setMargins(0, margin, 0, margin)
            // if (mOverlayView.getParent() == null)
            toggleOverlay(true)
            mFrame!!.addView(mGLSurfaceView, flp) // .addView(mGLSurfaceView);
            mK!!.setK3DMode(true, mK3DRenderer!!)
            mK!!.updateTexture()
            mK3DRenderer!!.start()
        } else if (mK3DRenderer != null) {
            mK!!.setK3DMode(false, null)
            toggleOverlay(false)
            mFrame!!.removeView(mGLSurfaceView)
            mK3DRenderer!!.stop()
            mGLSurfaceView = null
            mK3DRenderer = null
        }
    }

    internal inner class Export : AsyncTask<Bitmap?, Void?, String>() {


        override fun onPostExecute(result: String) {
            mK!!.toastString(result, Toast.LENGTH_LONG)
        }

        @SuppressLint("WrongThread")
        override fun doInBackground(vararg params: Bitmap?): String {
            return if (params.size == 0) mK!!.exportImage(null) else mK!!.exportImage(params[0])
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_PICTURE) {
            if (resultCode == RESULT_OK) {
                imageUri = data?.data
                val options = BitmapFactory.Options()
                options.inScaled = false
                loadBitmap(options, imageUri)
            }
        } else {
            val s = preferences!!.getString(KEY_IMAGE_URI, "")
            val b = s != sStringUri
            if (requestCode == CHANGE_NUMBER_OF_MIRRORS || b) {
                val numberOfMirrors = (KView.MIN_NOM + preferences!!.getInt(
                    KView.KEY_NUMBER_OF_MIRRORS, 6 - KView.MIN_NOM
                ))
                if (numberOfMirrors != mNumberOfMirrors || b) {
//					Intent intent = getIntent();
//					finish();
//					startActivity(intent);
                    mK!!.redraw()
                    return
                }
            }
            val blur = preferences!!.getBoolean(KView.KEY_BLUR, true)
            if (blur != mK!!.isBlur) mK!!.isBlur = blur
            if (blur) {
                val blurValue = (2.55 * (99.0 - preferences!!.getInt(
                    KView.KEY_BLUR_VALUE, 49
                ))).toInt()
                if (blurValue != mK!!.blurValue) mK!!.blurValue = blurValue
            }
            bCameraInMenu = preferences!!.getBoolean(KEY_CAMERA_IN_MENU, true)
        }
    }

    private fun fileExists(uri: Uri): Boolean {
        val filePath: String = try {
            getPath(uri)
        } catch (e: CursorIndexOutOfBoundsException) {
            return false
        } catch (e: NullPointerException) {
            return false
        }
        return File(filePath).exists()
    }

    private fun getPath(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    private fun toggleHardwareAcceleration(requestEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < 11) return
        if (!requestEnabled) {
            // Are there other views that can be set?
            mOverlayView!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            mK!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    fun toggleOverlay(enabled: Boolean) {
        if (enabled) {
            mFrame!!.addView(mOverlayView)
        } else {
            mFrame!!.removeView(mOverlayView)
        }
    }

    override fun onOptionsMenuClosed(menu: Menu) {
        super.onOptionsMenuClosed(menu)
        mMenu = null
    }

    override fun onBackPressed() {
        if (null == mMenu) {
            openOptionsMenu()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val KEY_IMAGE_URI = "image_uri"
        const val KEY_CAMERA_IN_MENU = "camera_in_menu"
        const val KEY_HARDWARE_ACCEL = "hardware_accel"

        // private static final String TAG = "Kaleidoscope";
        const val CHANGE_NUMBER_OF_MIRRORS = 1
        private const val OPEN_PICTURE = 2
        private var use3D = false
        private const val CAN_USE_CAMERA_PERMISSION_RESULT = 3
        const val CAN_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 4
    }
}