package vnd.blueararat.kaleidoscope6

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vnd.blueararat.kaleidoscope6.filters.SimplyRGB
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class KView @JvmOverloads constructor(
    context: Context, bitmap: Bitmap? = BitmapFactory.decodeResource(
        context.resources, R.drawable.transparent
    )
) : View(context), PreviewCallback {
    // private static final int CHANGE_NUMBER_OF_MIRRORS = 1;
    // private static final int OPEN_PICTURE = 2;
    // private static final int MIN_NOM = 2;
    var numberOfMirrors = 0
        private set
    private var mOffset = 0
    private val mTopOffset = -0.4f
    private var mAngle = 0f
    private var mAngle2 = 0f

    // private float mLocalAngle = 0;
    private var mBitmapNewHeight = 0
    private var mBitmapViewHeight = 0
    private var mRadius = 0
    private var mBitmapViewWidth = 0
    private var mBitmapViewWidthInitial = 0
    private var mCenterX = 0
    private var mCenterY = 0
    private var mScreenRadius = 0
    private var mScale = 0f
    private var mViewBitmap: Bitmap? = null
    private var mBitmap: Bitmap?

    // private Bitmap mScaledBitmap;
    // private static Uri imageUri;
    // private KaleidoscopeView k;
    // private String mStringUri = "";
    private var sWidth = 0
    private var sHeight = 0
    private var texX = 0
    private var texY = 0
    private var texWidth = 0
    private var mScaledHeight = 0
    private var mScaledWidth = 0
    private var mBitmapWidth = 0
    private var mBitmapHeight = 0
    private var mX = 0
    private var mY = 0

    // private float startX, startY;
    private var sX1 = 0f
    private var sY1 = 0f
    private var sD = 0f
    private var sMx = 0f
    private var sMy = 0f
    var mAlpha = false
    var mBlur = false
    var mAlphaMark = false
    var mBlurVal = 0
    private var msgNextTime = true

    // private int mFormat = 0;
    // private Menu mMenu;
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // private final Paint mPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private val mContext: Context

    // private Bitmap , mViewBitmap;
    // public int mNumberOfMirrors = super.mNumberOfMirrors;
    private var mCurX = 0
    private var mCurY = 0

    // private float mdX = 1;
    // private float mdY = 1;
    // private float mF;
    // private final Paint mPaint;
    // private Size previewImageSize;
    // private boolean processing;
    // private byte[] data;
    // private int[] rgb;
    // private int n = 0;
    // private String debugString = "";
    // private long lastFrame = System.currentTimeMillis();
    private var mDataLength = 0
    private var mPreviewWidth = 0
    private var mPreviewHeight = 0

    // private int mPictureWidth, mPictureHeight;
    private var preferences: SharedPreferences? = null
    private var use3D = false
    private var mK3DRenderer: K3DRenderer? = null
    fun setK3DMode(use3D: Boolean, mK3DRenderer: K3DRenderer?) {
        this.use3D = use3D
        this.mK3DRenderer = mK3DRenderer
    }

    private fun init() {
        preferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        numberOfMirrors = (MIN_NOM + preferences!!.getInt(KEY_NUMBER_OF_MIRRORS, 6 - MIN_NOM))
        mAngle = 180f / numberOfMirrors.toFloat()
        mAngle2 = mAngle * 2
        mOffset = calculateOffset(numberOfMirrors)
        mBlur = preferences!!.getBoolean(KEY_BLUR, true)
        mBlurVal = if (mBlur) {
            (2.55 * (99.0 - preferences!!.getInt(
                KEY_BLUR_VALUE, 49
            ))).toInt()
        } else {
            -1
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sHeight = height
        sWidth = width
        if (sHeight == 0 || sWidth == 0) {
            val mMetrics = DisplayMetrics()
            val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getMetrics(mMetrics)
            sHeight = mMetrics.heightPixels
            sWidth = mMetrics.widthPixels
        }
        if (sHeight < sWidth) {
            texY = 0
            texX = (sWidth - sHeight) / 2
            texWidth = sHeight
        } else {
            texY = (sHeight - sWidth) / 2
            texX = 0
            texWidth = sWidth
        }
        mBitmapViewWidth = (sWidth / 2)
        mCenterX = mBitmapViewWidth
        mCenterY = sHeight / 2
        mScreenRadius = Math.min(mBitmapViewWidth, sHeight / 2)
        // mBitmap.getHeight()));
        drawingCacheBackgroundColor = Color.TRANSPARENT
        updateBitmapInfo()
        drawIntoBitmap()
    }

    // private Bitmap rotatedBitmap(float angle, Bitmap bm) {
    // Matrix matrix = new Matrix();
    // float px = bm.getWidth();
    // float py = bm.getHeight();
    // // matrix.postRotate(mLocalAngle, mBitmapViewWidth, mBitmapViewWidth);
    // matrix.postRotate(angle, px / 2, py / 2);
    // Bitmap bmr = Bitmap.createBitmap(bm, 0, 0, (int) px, (int) py,
    // matrix, true);
    // return bmr;
    // }
    private fun drawIntoBitmap() {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (mAlpha) {
            if (!mBlur) {
                mViewBitmap!!.eraseColor(Color.TRANSPARENT)
            }
        }
        if (mBlur) {
            if (mAlphaMark) {
                p.alpha = mBlurVal
            } else {
                mAlphaMark = true
            }
        }
        val c = mViewBitmap?.let { Canvas(it) }
        c?.save()
        val path = Path()
        path.moveTo(0f, 0f)
        path.arcTo(
            RectF(
                -mBitmapViewWidth.toFloat(),
                -mBitmapViewWidth.toFloat(),
                mBitmapViewWidth.toFloat(),
                mBitmapViewWidth.toFloat()
            ), 0f, mAngle
        )
        // path.lineTo(0, 1);
        path.close()
        c?.clipPath(path)
        // c.drawRGB(0, 0, 0);
        // RectF r = new RectF(0, 0, mBitmapViewWidth, mBitmapViewHeight);

        Log.d(TAG, "drawIntoBitmap: mCurX  $mCurX  ---- mCurY $mCurY")
        mBitmap?.let {
            c?.drawBitmap(
                it, Rect(
                    mCurX, mCurY, mRadius + mCurX, mBitmapNewHeight + mCurY
                ), RectF(
                    0f, 0f, mBitmapViewWidth.toFloat(), mBitmapViewHeight.toFloat()
                ), p
            )
        }
    }

    private fun drawFrame() {
        val c = mViewBitmap?.let { Canvas(it) }
        // Paint p = new Paint();
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (mBlur) {
            if (mAlphaMark) {
                p.alpha = mBlurVal
            } else {
                mAlphaMark = true
            }
        }
        // p.setAntiAlias(true);
        c?.save()
        val path = Path()
        path.moveTo(0f, 0f)
        path.arcTo(
            RectF(
                -mBitmapViewWidth.toFloat(),
                -mBitmapViewWidth.toFloat(),
                mBitmapViewWidth.toFloat(),
                mBitmapViewWidth.toFloat()
            ), 0f, mAngle
        )
        // path.lineTo(0, 1);
        path.close()
        c?.clipPath(path)
        // RectF r = new RectF(0, 0, mBitmapViewWidth, mBitmapViewHeight);
        mBitmap?.let {
            c?.drawBitmap(
                it, Rect(
                    mCurX, mCurY, mRadius + mCurX, mBitmapNewHeight + mCurY
                ), RectF(
                    0f, 0f, mBitmapViewWidth.toFloat(), mBitmapViewHeight.toFloat()
                ), p
            )
        }
        // mPaint.setTextSize(28);
        // mPaint.setColor(Color.RED);
        // c.drawText(debugString, 150, 50, mPaint);
    }

    private fun drawIntoBitmap(
        bm: Bitmap?, initialBitmap: Bitmap?, posX: Int, posY: Int
    ) {
        val x = bm!!.width
        val y = bm.height
        val c = Canvas(bm)
        c.save()
        val path = Path()
        path.moveTo(0f, 0f)
        path.arcTo(RectF(-x.toFloat(), -x.toFloat(), x.toFloat(), x.toFloat()), 0f, mAngle)
        // path.lineTo(0, 1);
        path.close()
        c.clipPath(path)
        // RectF r = new RectF(0, 0, x, y);
        if (initialBitmap != null) {
            c.drawBitmap(
                initialBitmap, Rect(
                    posX, posY, mRadius + posX, mBitmapNewHeight + posY
                ), RectF(0f, 0f, x.toFloat(), y.toFloat()), mPaint
            )
        }
    }

    fun exportImage(bmp: Bitmap?): String {
        val g: Bitmap.Config
        val cf: CompressFormat
        var ext = preferences!!.getString(
            "format", mContext.getString(R.string.default_save_format)
        )
        val format = if (ext == "JPEG") 1 else 0
        ext = if (format == 1) ".jpg" else ".png"
        val q: Int
        if (format == 0) {
            g = Bitmap.Config.ARGB_8888
            cf = CompressFormat.PNG
            q = 100
        } else {
            g = Bitmap.Config.RGB_565
            cf = CompressFormat.JPEG
            q = 50 + preferences!!.getInt("jpeg_quality", 40)
        }
        var exportBitmap: Bitmap? = null
        if (bmp == null) {
            val x: Int
            val rad: Int
            val factor: Int
            var newBitmap = mViewBitmap
            if (!mBlur) {
                factor = Memory.factor()
                newBitmap = Bitmap.createBitmap(
                    mRadius / factor, mBitmapNewHeight / factor, Bitmap.Config.ARGB_8888
                )
                // Bitmap bm = rotatedBitmap(mLocalAngle, mBitmap);
                drawIntoBitmap(newBitmap, mBitmap, mCurX, mCurY)
                x = mRadius * 2 / factor
                rad = mRadius / factor
            } else {
                x = mBitmapViewWidth * 2
                rad = mBitmapViewWidth
            }
            exportBitmap = Bitmap.createBitmap(x, x, g)
            val c = Canvas(exportBitmap)
            paint(c, newBitmap, rad)
            if (!mBlur) newBitmap!!.recycle()
            System.gc()
        } else {
            exportBitmap = bmp
        }
        var path = preferences!!.getString(Prefs.KEY_FOLDER, "")
        if (path == "") {
            path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).toString()
        }
        val directory = File(path, "Kaleidoscope")
        directory.mkdirs()
        // File directory = new File(path);

        // MediaStore.Images.Media.insertImage(null, mBitmap2, null, null);
        val s = SimpleDateFormat("MMddmmss")
        val stamp = s.format(Date())
        val file = File(
            directory, mContext.getString(R.string.png_save_name) + stamp + ext
        )
        var stream: ByteArrayOutputStream? = ByteArrayOutputStream()
        exportBitmap!!.compress(cf, q, stream)
        var byteArray = stream!!.toByteArray()
        stream = null
        exportBitmap.recycle()
        System.gc()
        var out: BufferedOutputStream? = null
        try {
            out = BufferedOutputStream(FileOutputStream(file))
            out.write(byteArray)
        } catch (e: Exception) {
            // Log.e(TAG, "Failed to write image", e);
        } finally {
            try {
                out!!.close()
            } catch (e: Exception) {
                // e.printStackTrace();
            }
        }
        byteArray = null
        System.gc()
        val toast1: String
        if (file.exists()) {
            toast1 = (mContext.getString(R.string.picture_saved_to) + " " + file.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val mediaScanIntent = Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
                )
                val contentUri = Uri.fromFile(file)
                mediaScanIntent.data = contentUri
                mContext.sendBroadcast(mediaScanIntent)
            } else {
                mContext.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_MOUNTED, Uri.parse(
                            "file://" + Environment.getExternalStorageDirectory()
                        )
                    )
                )
            }
        } else {
            toast1 =
                (mContext.getString(R.string.cant_save_picture_to) + " " + directory.toString() + mContext.getString(
                    R.string.please_change_folder
                ))
        }
        return toast1
    }

    fun toastString(s: String?, duration: Int) {
        Toast.makeText(mContext, s, duration).show()
    }

    fun setViewBitmapSizes(width: Int) {
        var width = width
        if (-1 == width) {
            width = mBitmapViewWidth
        }
        val diam = 2 * width
        if (Memory.checkBitmapFitsInMemory(diam, diam)) {
            mBitmapViewWidth = width
            mBitmapViewHeight =
                Math.round(width.toDouble() * Math.sin(Math.PI / numberOfMirrors.toDouble()))
                    .toInt()
            mScale = mBitmapViewWidth.toFloat() / mRadius
            mScaledHeight = (mScale * mBitmapHeight).toInt()
            mScaledWidth = (mScale * mBitmapWidth).toInt()
            mAlphaMark = false
            mViewBitmap = Bitmap.createBitmap(
                mBitmapViewWidth, mBitmapViewHeight, Bitmap.Config.ARGB_8888
            )
            mX = mScaledWidth - mBitmapViewWidth
            mY = mScaledHeight - mBitmapViewHeight
            msgNextTime = true
        } else if (msgNextTime) {
            toastString(
                mContext.getString(R.string.cant_zoom_in), Toast.LENGTH_SHORT
            )
            msgNextTime = false
        }
    }

    fun updateBitmapInfo() {
        mBitmapWidth = mBitmap!!.width
        mBitmapHeight = mBitmap!!.height
        mRadius = (mBitmapWidth / 2)
        mBitmapNewHeight =
            Math.round(mRadius.toDouble() * Math.sin(Math.PI / numberOfMirrors.toDouble())).toInt()
        if (mBitmapNewHeight > mBitmapHeight) {
            mBitmapNewHeight = mBitmapHeight / 2
            mRadius =
                Math.round(mBitmapNewHeight.toDouble() / Math.sin(Math.PI / numberOfMirrors.toDouble()))
                    .toInt()
        }
        mScale = mBitmapViewWidth.toFloat() / mRadius
        // Log.i(TAG,String.format("%d", mBitmapViewWidth));
        mBitmapViewHeight = Math.round(mBitmapNewHeight.toFloat() * mScale)
        mScaledHeight = (mScale * mBitmapHeight).toInt()
        mScaledWidth = (mScale * mBitmapWidth).toInt()
        // Log.i(TAG,String.format("%d %d", mRadius, mBitmapNewHeight));
        mAlphaMark = false
        mViewBitmap = Bitmap.createBitmap(
            mBitmapViewWidth, mBitmapViewHeight, Bitmap.Config.ARGB_8888
        )
        mX = mScaledWidth - mBitmapViewWidth
        mY = mScaledHeight - mBitmapViewHeight
        mCurX = (Math.random() * mX / mScale).toInt()
        mCurY = (Math.random() * mY / mScale).toInt()
        mAlpha = mBitmap!!.hasAlpha()
        // Toast.makeText(mContext, Boolean.toString(mAlpha),
        // Toast.LENGTH_SHORT)
        // .show();
    }

    fun setBitmap(bitmap: Bitmap?) {
        mBitmap = bitmap
    }

    fun redraw(bitmap: Bitmap?) {
        setBitmap(bitmap)
        init()
        updateBitmapInfo()
        drawIntoBitmap()
    }

    fun redraw() {
        init()
        updateBitmapInfo()
        drawIntoBitmap()
    }

    // public void setFormat(int format) {
    // mFormat = format;
    // }
    var isBlur: Boolean
        get() = mBlur
        set(blur) {
            mBlur = blur
            mAlphaMark = false
        }
    var blurValue: Int
        get() = mBlurVal
        set(blurValue) {
            mBlurVal = blurValue
            mAlphaMark = false
        }

    @Synchronized
    private fun paint(canvas: Canvas, bitmap: Bitmap?) {
        // Paint p = new Paint();
        // p.setAntiAlias(true);
        // canvas.drawColor(Color.BLACK);
        // canvas.drawRGB(0, 0, 0);
        // canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        // mPaint.setTextSize(28);
        // Paint paint1 = new Paint();
        // paint1.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        // canvas.drawPaint(paint1);
        // paint1.setXfermode(new PorterDuffXfermode(Mode.SRC));
        // start your own drawing
        // canvas.save();
        //
        // // mPaint.setStyle(Paint.Style.FILL);
        // // // mPaint.setStrokeWidth(4);
        // // mPaint.setColor(Color.BLACK);
        // // canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
        // // mPaint);
        // canvas.drawRGB(255, 0, 0);
        // canvas.restore();
        var angle = mStartAngle
        for (i in 0 until numberOfMirrors) {
            canvas.save() // LayerAlpha(0, 0, mBitmapViewWidth,
            // mBitmapViewWidth,
            // 0xFF, LAYER_FLAGS);
            canvas.translate(mCenterX.toFloat(), mCenterY.toFloat())

            canvas.rotate(angle)
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, mOffset.toFloat(), mTopOffset, mPaint)

            }
            // p.setColor(Color.RED);
            // canvas.drawText(Integer.toString(i), 100, 30, p);
            // canvas.drawArc(new RectF(-200, -200, 200, 200), 0, mAngle,
            // false, p);
            canvas.scale(1f, -1f)
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, mOffset.toFloat(), mTopOffset, mPaint)
            }
            // p.setColor(Color.GREEN);
            // canvas.drawText(Integer.toString(i), 100, 30, p);
            // canvas.drawArc(new RectF(-150, -150, 150, 150), 0, mAngle,
            // false, p);
            canvas.restore()
            Log.d(TAG, "paint: Offset   $mOffset --- $mTopOffset")
            Log.d(TAG, "paint: mcenterX: ${mCenterX} --- mCenterY $mCenterY")
            angle += mAngle2
        }
    }

    @Synchronized
    private fun paint(canvas: Canvas, bitmap: Bitmap?, radius: Int) {
        val p = Paint()
        p.isAntiAlias = true
        var angle = mStartAngle
        for (i in 0 until numberOfMirrors) {
            canvas.save()
            canvas.translate(radius.toFloat(), radius.toFloat())
            canvas.rotate(angle)
            if (bitmap != null) {
                //canvas.drawBitmap(bitmap, mOffset.toFloat(), mTopOffset, mPaint)
            }
            canvas.scale(1f, -1f)
            if (bitmap != null) {
                //canvas.drawBitmap(bitmap, mOffset.toFloat(), mTopOffset, mPaint)
            }
            canvas.restore()
            angle += mAngle2
        }
    }

    override fun onDraw(canvas: Canvas) {
        // setDrawingCacheEnabled(false);
        paint(canvas, mViewBitmap)
    }

    var count =0 
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val P = event.pointerCount
        // int N = event.getHistorySize();
        count ++

        if (action == MotionEvent.ACTION_UP){
            count = 0
        }
        Log.d(TAG, "onTouchEvent: Count $count")
        if (action != MotionEvent.ACTION_MOVE) {

            if (action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "action down: ")
                Log.d(TAG, "onTouchEventBefore: mCurX $mCurX --- mCurY $mCurY")
                // startX = mCurX * mScale; // event.getX();
                // startY = mCurY * mScale; // event.getY();
                sX1 = mCurX * mScale - event.getX(0)
                sY1 = mCurY * mScale - event.getY(0)
                Log.d(TAG, "onTouchEvent: mCurX $mCurX --- mCurY $mCurY")
                return true
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN) {

                if (P == 2) {
                    Log.d(TAG, "down action p2 zoom: ")
                    val sX2 = event.getX(0) - event.getX(1)
                    val sY2 = event.getY(0) - event.getY(1)
                    sD = Math.sqrt((sX2 * sX2 + sY2 * sY2).toDouble()).toFloat()
                    mBitmapViewWidthInitial = mBitmapViewWidth
                    return true
                } else if (P == 3) {
                    Log.d(TAG, "down action p3: ")
                    sMx = mCenterX.toFloat() - event.getX(2)
                    sMy = mCenterY.toFloat() - event.getY(2)
                    return true
                }
                return false
            }
        } else {
            // action move
            if (P == 1) {
                //xoay ảnh
                Log.d(TAG, "action p1: ")
                val a = Math.abs(sX1 + event.x) % mX
                val b = Math.abs(sY1 + event.y) % mY

                Log.d(TAG, "onTouchEvent: mx : $mX, my : $mY")
                Log.d(TAG, "position x: " + sX1 + ", y: " + sY1)
                mCurX =
                    (a / mScale).toInt() // *(int)(event.getHistoricalX(0)-event.getHistoricalX(1));
                mCurY =
                    (b / mScale).toInt() // *(int)(event.getHistoricalY(0)-event.getHistoricalY(1));


            } else if (P == 2) {
                Log.d(TAG, "action p2: ")
                //zoom ảnh
                val sX2 = event.getX(0) - event.getX(1)
                val sY2 = event.getY(0) - event.getY(1)
                val sD2 = Math.sqrt((sX2 * sX2 + sY2 * sY2).toDouble()).toFloat()
                var r = mBitmapViewWidthInitial + Math.round(sD2 - sD)
                if (r < mScreenRadius) r = mScreenRadius
                setViewBitmapSizes(r)
            } else if (P == 3) {
                Log.d(TAG, "action p3: ")
                mCenterX = (sMx + event.getX(2)).toInt()
                mCenterY = (sMy + event.getY(2)).toInt()
            } else {
                return false
            }
            drawIntoBitmap()
            invalidate()
            if (use3D) {
                updateTexture()
            }
        }
        return true
    }


    fun calculateOctagonPoints(screenWith: Int, screenHeight: Int): List<Point> {
        val octagonRadius = 0.4 * Math.min(screenWith, screenHeight)

        val centerX = screenWith / 2
        val centerY = screenHeight / 2

        val numPoints = 8

        val listPointOct = mutableListOf<Point>()
        val listMotiontOct = mutableListOf<Point>()

        // Tính toán các điểm trên hình bát giác
        for (i in 0 until numPoints) {
            val octagonTheta = 2.0 * Math.PI * i / numPoints
            val octagonX = (centerX + octagonRadius * Math.cos(octagonTheta)).toInt()
            val octagonY = (centerY + octagonRadius * Math.sin(octagonTheta)).toInt()
            listPointOct.add(Point(octagonX, octagonY))

        }
        val pointA = listPointOct[0]
        val pointB = listPointOct[1]
        val pointC = listPointOct[2]
        val pointD = listPointOct[3]
        val pointE = listPointOct[4]
        val pointF = listPointOct[5]
        val pointG = listPointOct[6]
        val pointH = listPointOct[7]

        val newListAB = mutableListOf<Point>()
        val newListBC = mutableListOf<Point>()
        val newListCD = mutableListOf<Point>()
        val newListDE = mutableListOf<Point>()
        val newListEF = mutableListOf<Point>()
        val newListFG = mutableListOf<Point>()
        val newListGH = mutableListOf<Point>()
        val newListHA = mutableListOf<Point>()


        newListAB.addAll(generatePointsBetweenTwoPointsAtoB(pointA, pointB, 30))
        newListGH.addAll(generatePointsBetweenTwoPointsAtoB(pointG, pointH, 30))
        newListBC.addAll(generatePointsBetweenTwoPointsBtoA(pointB, pointC, 30))
        newListCD.addAll(generatePointsBetweenTwoPointsBtoA(pointC, pointD, 30))
        newListDE.addAll(generatePointsBetweenTwoPointsBtoA(pointD, pointE, 30))
        newListEF.addAll(generatePointsBetweenTwoPointsBtoA(pointE, pointF, 30))
        newListFG.addAll(generatePointsBetweenTwoPointsBtoA(pointF, pointG, 30))
        newListHA.addAll(generatePointsBetweenTwoPointsBtoA(pointH, pointA, 30))



        listMotiontOct.addAll(newListAB)
        listMotiontOct.addAll(newListBC)
        listMotiontOct.addAll(newListCD)
        listMotiontOct.addAll(newListDE)
        listMotiontOct.addAll(newListEF)
        listMotiontOct.addAll(newListFG)
        listMotiontOct.addAll(newListGH)
        listMotiontOct.addAll(newListHA)
        return listMotiontOct
    }


    fun generatePointsBetweenTwoPointsAtoB(
        a: Point, b: Point, numberOfPoints: Int
    ): List<Point> {
        val generatedPoints = mutableListOf<Point>()

        // Tính toán khoảng cách giữa các điểm
        val deltaX = (b.x - a.x).toDouble() / (numberOfPoints - 1)
        val deltaY = (b.y - a.y).toDouble() / (numberOfPoints - 1)

        // Tạo các điểm trên đoạn thẳng
        for (i in 0 until numberOfPoints) {
            val x = (a.x + i * deltaX).toInt()
            val y = (a.y + i * deltaY).toInt()
            val motionEventWrapper = Point()
            motionEventWrapper.y = y
            motionEventWrapper.x = x
            generatedPoints.add(motionEventWrapper)
        }
        return mutableListOf()

    }


    fun generatePointsBetweenTwoPointsBtoA(
        a: Point, b: Point, numberOfPoints: Int
    ): List<Point> {
        val generatedPoints = mutableListOf<Point>()

        val deltaX = (b.x - a.x).toDouble() / (numberOfPoints - 1)
        val deltaY = (b.y - a.y).toDouble() / (numberOfPoints - 1)

        for (i in 0 until numberOfPoints) {
            val x = (a.x + i * deltaX).toInt()
            val y = (a.y + i * deltaY).toInt()
            val motionEventWrapper = Point()
            motionEventWrapper.y = y
            motionEventWrapper.x = x

            generatedPoints.add(motionEventWrapper)
        }

        return generatedPoints
    }


    var jobDraw: Job? = null

    var isPaused: Boolean = false
    var x = 0;
    var y = 568
    fun drawPoints(width: Int, height: Int) {
        jobDraw?.cancel()

        jobDraw = GlobalScope.launch(Dispatchers.Main) {
            val job1 = launch(Dispatchers.Main) {
              while(true) {
//                    Log.d(TAG, "drawPoints: x  ${it.x}, y ${it.y}")

//                    Log.d(TAG, "drawPoints: x  ${x}, y ${y}")
                    if (!isPaused) {
                        Log.d(TAG, " draw action p1: ")
                        x += 10
                        y += 10
                        val a = Math.abs(sX1 +x ) % mX
                        val b = Math.abs(sY1 + y) % mY
                        mCurX =
                            (a / mScale).toInt() // *(int)(event.getHistoricalX(0)-event.getHistoricalX(1));
                        mCurY =
                            (b / mScale).toInt() // *(int)(event.getHistoricalY(0)-event.getHistoricalY(1));

                        drawIntoBitmap()

                        if (use3D) {
                            updateTexture()
                        }

                    }
                    delay(15)
                    invalidate()
                }
            }
            job1.join()

        }

    }


    fun updateTexture() {
        isDrawingCacheEnabled = true
        buildDrawingCache(true)
        try {
            mK3DRenderer!!.setBitmap(
                Bitmap.createBitmap(
                    drawingCache, texX, texY, texWidth, texWidth
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isDrawingCacheEnabled = false
    }

    fun resetSizes(width: Int, height: Int) {
        // mPictureWidth = pictureWidth;
        // mPictureHeight = pictureHeight;
        mDataLength = width * height
        mPreviewWidth = width
        mPreviewHeight = height
        mBitmapWidth = width
        mBitmapHeight = height
        mRadius = (mBitmapWidth / 2)
        mBitmapNewHeight = (mRadius.toDouble() * Math.sin(
            Math.PI / numberOfMirrors.toDouble()
        )).toInt()
        if (mBitmapNewHeight > mBitmapHeight) {
            mBitmapNewHeight = mBitmapHeight / 2
            mRadius =
                Math.round(mBitmapNewHeight.toDouble() / Math.sin(Math.PI / numberOfMirrors.toDouble()))
                    .toInt()
        }
        mScale = mBitmapViewWidth.toFloat() / mRadius
        mScaledWidth = (mScale * mBitmapWidth).toInt()
        mScaledHeight = (mScale * mBitmapHeight).toInt()
        mX = mScaledWidth - mBitmapViewWidth
        mY = mScaledHeight - mBitmapViewHeight
        mCurX = (Math.random() * mX / mScale).toInt()
        mCurY = (Math.random() * mY / mScale).toInt()
    }

    var yUVProcessor: YUVProcessor = SimplyRGB()

    init {
        isFocusable = true
        mContext = context
        mBitmap = bitmap
        init()
    }

    fun currentYUVProcessor(): Int {
        for (i in YUVProcessor.YUV_PROCESSORS.indices) {
            val yp = YUVProcessor.YUV_PROCESSORS[i]
            if (yp === yUVProcessor) return i
        }
        throw Error("")
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        val rgb = IntArray(mDataLength)
        yUVProcessor.processYUV420SP(rgb, data, mPreviewWidth, mPreviewHeight)
        val bmp = Bitmap.createBitmap(
            rgb, mPreviewWidth, mPreviewHeight, Bitmap.Config.ARGB_8888
        )
        setBitmap(bmp)
        // n++;
        // if (n == 10) {
        // debugString = Double
        // .toString(10000 / (System.currentTimeMillis() - lastFrame))
        // + " FPS";
        // lastFrame = System.currentTimeMillis();
        // n = 0;
        // }
        drawFrame()
        invalidate()
        if (use3D) {
            updateTexture()
        }
        camera.addCallbackBuffer(data)
    }

    fun setNewSettings(number_of_mirrors: Int) {
        numberOfMirrors = number_of_mirrors
        mAngle = 180f / numberOfMirrors.toFloat()
        mAngle2 = mAngle * 2
        mOffset = calculateOffset(numberOfMirrors)
        // double d = Math.sin(Math.PI / (double) mNumberOfMirrors);
        mBitmapNewHeight =
            Math.round(mRadius.toDouble() * Math.sin(Math.PI / numberOfMirrors.toDouble())).toInt()
        mBitmapViewHeight = Math.round(mBitmapNewHeight.toFloat() * mScale)
        mAlphaMark = false
        mViewBitmap = Bitmap.createBitmap(
            mBitmapViewWidth, mBitmapViewHeight, Bitmap.Config.ARGB_8888
        )
        mY = mScaledHeight - mBitmapViewHeight
        mCurY = (Math.random() * mY / mScale).toInt()
    }

    fun destroy() {
        if (mViewBitmap != null) mViewBitmap!!.recycle()
        if (mBitmap != null) mBitmap!!.recycle()
        if (mK3DRenderer != null) {
            mK3DRenderer!!.stop()
            mK3DRenderer = null
        }
        try {
            System.gc()
            System.gc()
        } catch (e: NullPointerException) {
        }
    } // public void reset(Size previewImageSize) {

    // processing = false;
    // rgb = new int[0];
    // data = new byte[0];
    // this.previewImageSize = previewImageSize;
    // }
    companion object {
        private const val TAG = "KView"

        // Camera.PictureCallback
        // private static final int LAYER_FLAGS = Canvas.MATRIX_SAVE_FLAG
        // | Canvas.CLIP_SAVE_FLAG | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
        // | Canvas.FULL_COLOR_LAYER_SAVE_FLAG
        // | Canvas.CLIP_TO_LAYER_SAVE_FLAG;
        // SharedPreferences preferences;
        // static final String KEY_NUMBER_OF_MIRRORS = "number_of_mirrors";
        // static final String KEY_IMAGE_URI = "image_uri";
        // private static final String TAG = "KView";
        // private final float SCALE = getResources().getDisplayMetrics().density;
        const val MIN_NOM = 2

        // static final int MIN_ALPHA_VALUE = 1;
        const val KEY_NUMBER_OF_MIRRORS = "number_of_mirrors"
        const val KEY_BLUR = "blur"
        const val KEY_BLUR_VALUE = "blur_value"
        private const val mStartAngle = 0f
        private fun calculateOffset(nom: Int): Int {
            var offset = (-1.0 / Math.tan(Math.PI / nom.toDouble())).toInt()
            if (nom == 3 || nom == 5 || nom == 9 || nom == 14 || nom == 15 || nom == 19 || nom == 20 || nom == 21 || nom == 22 || nom == 24) {
                offset = offset - 1
            } else if (nom == 25) {
                offset = offset - 2
            }
            return offset
        }
    }
}