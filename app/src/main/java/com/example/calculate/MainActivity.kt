package com.example.calculate


import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.calculate.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addSpace()
        submitBtn()
        cameraApp()
    }

    var addCnt = 0

    ////////////////////////////////////// 메인화면
    private fun addSpace() {
        var space1 = findViewById<LinearLayout>(R.id.addIndividualPrice1)
        var space2 = findViewById<LinearLayout>(R.id.addIndividualPrice2)
        var space3 = findViewById<LinearLayout>(R.id.addIndividualPrice3)
        var space4 = findViewById<LinearLayout>(R.id.addIndividualPrice4)
        var space5 = findViewById<LinearLayout>(R.id.addIndividualPrice5)
        var space6 = findViewById<LinearLayout>(R.id.addIndividualPrice6)
        var space7 = findViewById<LinearLayout>(R.id.addIndividualPrice7)
        var space8 = findViewById<LinearLayout>(R.id.addIndividualPrice8)
        var space9 = findViewById<LinearLayout>(R.id.addIndividualPrice9)
        var space10 = findViewById<LinearLayout>(R.id.addIndividualPrice10)

        var array = arrayOf(
            space1, space2, space3, space4, space5,
            space6, space7, space8, space9, space10
        )

        var addSpace = findViewById<TextView>(R.id.addSpace)

        addSpace.setOnClickListener {
            for (i in array) {
                if (i.visibility == View.GONE) {
                    i.visibility = View.VISIBLE
                    addCnt++
                    if (addCnt == 10) {
                        addSpace.visibility = View.GONE
                    }
                    break
                }
            }
        }
    }

    // 버튼이벤트로 result페이지로 넘어가는 메소드.
    private fun submitBtn() {
        val submitBtn = findViewById<TextView>(R.id.submitBtn)
        submitBtn.setOnClickListener {
            if (validationCheck().equals("blank")) {
                alert("입력오류", "필수항목을 입력해주세요")
            } else if (validationCheck().equals("trim")) {
                alert("공백오류", "입력값에 공백을 포함할 수 없습니다")
            } else {
                arrayResult()
                for(i in 0.. finalResList.size-1) {
                    if(finalResList[i].name == null || finalResList[i].kind == null || finalResList[i].price == null) {
                        alert("공백오류", "추가금액에 입력값을 입력해주세요")
                    }
                }
                calculRes(finalResList)
                setContentView(R.layout.result)
                resView()
            }
        }
    }

    // 유효성검사를 통해서 공백값이나 빈칸일때 반환값을 넘기는 함수.
    // 공백검사도 진행했는데,
    // 어차피 레이아웃에서 인풋타입을 넘버로주면 숫자만 입력할 수 있다고한다.
    private fun validationCheck() : String? {
        val heads = findViewById<EditText>(R.id.headCount)
        val prices = findViewById<EditText>(R.id.overallPrice)
        if (heads.text.isBlank() || prices.text.isBlank()) {
            return "blank"
        }
        if (heads.text.contains(" ") || prices.text.contains(" ")) {
            return "trim"
        }
        return null
    }

    // 유효성검사 실시 후, 올바른 입력값이 아닐때 띄워줄 알러트.
    private fun alert(setTitle : String, setMessage : String) {
        var alertBlank = AlertDialog.Builder(this).apply {
            setTitle(setTitle)
            setMessage(setMessage)
            setPositiveButton("확인", DialogInterface.OnClickListener { dialog, which ->
                Toast.makeText(this.context, "확인", Toast.LENGTH_SHORT).show()
            })
            create()
            show()
        }
    }

    lateinit var binding: ActivityMainBinding
    lateinit var filePath: String
    var imagePath : String? = null
    var savedBitmap: Bitmap? = null

    // 카메라와 갤러리를 사용하여 영수증을 올릴 수 있도록하는 함수.
    // 컨텐트 프로바이더를 사용한다.
    private fun cameraApp() {
        // 바인딩을 하기위한 선언.
        // 초기화미루기를 했던 값에 초기화해준다.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        addSpace()
        submitBtn()

        // 이미지 갤러리 요청
        val requestGalleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            try {
                val calRatio = calculateInSampleSize(
                    it.data!!.data!!,
                    resources.getDimensionPixelSize(R.dimen.imgWidth),
                    resources.getDimensionPixelSize(R.dimen.imgHeight)
                )
                val option = BitmapFactory.Options()
                option.inSampleSize = calRatio
                // 이미지 로딩
                var inputStream = contentResolver.openInputStream(it.data!!.data!!)
                // 인풋스트림에서 받아온 값, 패딩값(?), 옵션에서 설정한 사이즈값을 넣는다.
                val bitmap = BitmapFactory.decodeStream(inputStream, null, option)
                // 인풋스트림을 닫아주고, null로 초기화를 해줘서 데이터누수를 막는 것 같다.
                inputStream!!.close()
                inputStream = null
                bitmap?.let {
                    binding.billImage.setImageBitmap(bitmap)
                }
                // 이미지경로는 전역변수에 저장시켜서 페이지 이동시에도 사용할 예정이다.
                imagePath = saveBitmapToFile(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.billGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type="image/*"
            requestGalleryLauncher.launch(intent)
        }

        // 갤러리쪽에서 작업했던 내용과 비슷한 맥락이다.
        val requestCameraFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){

            val calRatio = calculateInSampleSize(
                Uri.fromFile(File(filePath)),
                resources.getDimensionPixelSize(R.dimen.imgWidth),
                resources.getDimensionPixelSize(R.dimen.imgHeight)
            )
            val option = BitmapFactory.Options()
            option.inSampleSize = calRatio
            val bitmap = BitmapFactory.decodeFile(filePath, option)
            bitmap?.let {
                binding.billImage.setImageBitmap(bitmap)
            }
            imagePath = saveBitmapToFile(bitmap)
        }

        binding.billPicture.setOnClickListener {
            // timeStamp가 이곳에서 어떤의미로 사용되고 있는지는 정확히 모르겠음.
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            filePath = file.absolutePath

            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.calculate_provider.fileprovider",
                file
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            requestCameraFileLauncher.launch(intent)
        }
    }

    // 비트맵의 절대 경로를 반환해준다. (마지막 로직이 file.absolutePath이면서 반환값이 String임.)
    private fun saveBitmapToFile(bitmap: Bitmap?): String? {
        return try {
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "your_image_filename.jpg")
            val stream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // 파일경로값과 가로세로 폭을 받아서 이미지의 비율을 반환해주는 것 같다.
    private fun calculateInSampleSize(fileUri : Uri, reqWidth : Int, reqHeight : Int) :Int {
        val option = BitmapFactory.Options()
        option.inJustDecodeBounds = true
        try {
            var inputStream = contentResolver.openInputStream(fileUri)
            BitmapFactory.decodeStream(inputStream, null,option)
            inputStream!!.close()
            inputStream = null
        } catch (e : Exception) {
            e.printStackTrace()
        }
        val(height : Int, width : Int) = option.run { outHeight to outWidth }
        var inSampleSize = 1
        // inSampleSize 비율계산
        if (height > reqHeight || width > reqWidth) {
            val halfHeight : Int = height / 2
            val halfWidth : Int = width / 2
            while(halfHeight / inSampleSize >= reqHeight &&
                    halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    var finalPrice : Int = 0
    var finalIndPrice : Int = 0
    var finalHeadCount : Int = 0
    lateinit var finalResList : MutableList<ResData>

    // 2차원 배열과 같은 형태의 데이터들을 모아줄 클래스.
    data class ResData(val name :String, val kind : String, val price : Int)

    private fun arrayResult() {

        var resList = mutableListOf<ResData>()
        // 이런식으로 설정을 해놓으면 동적으로 아이디값을 가져올 수 있다.
        // 이전부터 계속 궁금했는데 찾았더니 나왔다.
        for (i in 1..10) {
            var layoutNo = findViewById<LinearLayout>(resources.getIdentifier("addIndividualPrice$i", "id", "com.example.calculate"))
            var nameNo = findViewById<EditText>(resources.getIdentifier("indName$i", "id", "com.example.calculate")).text
            var kindNo = findViewById<EditText>(resources.getIdentifier("indKind$i", "id", "com.example.calculate")).text
            var priceNo = findViewById<EditText>(resources.getIdentifier("indPrice$i", "id", "com.example.calculate")).text
            if(layoutNo.visibility != View.GONE) {
                var data = ResData (
                    nameNo.toString(),
                    kindNo.toString(),
                    priceNo.toString().toInt()
                )
                resList.add(data)
            }
        }
        finalResList = resList
    }

    // 정산결과를 계산하고, 전역변수에 담아준다.
    private fun calculRes(resList : MutableList<ResData>) {

        var totalIndPrice : Int = 0
        if(resList.size > 0) {
            for (i in 0..resList.size - 1) {
                totalIndPrice += resList[i].price
            }
        }

        var totalPrice : Int = findViewById<EditText>(R.id.overallPrice).text.toString().toInt()
        finalPrice = totalPrice-totalIndPrice
        finalHeadCount = findViewById<EditText>(R.id.headCount).text.toString().toInt()
        finalIndPrice = finalPrice/finalHeadCount
    }


    //////////////////////////////////////////계산결과화면
    private fun resView() {
        findViewById<TextView>(R.id.resOverallPrice).setText("총 가격 : ${finalPrice}원")
        findViewById<TextView>(R.id.resHeadCount).setText("총 인원 : ${finalHeadCount}명")
        findViewById<TextView>(R.id.resIndPrice).setText("인당 금액 : ${finalIndPrice}원")

        savedBitmap = BitmapFactory.decodeFile(imagePath)
        var resBillImageView = findViewById<ImageView>(R.id.resBill)
        resBillImageView.setImageBitmap(savedBitmap)

        for(i in 1.. finalResList.size) {
            var addprice = findViewById<TextView>(resources.getIdentifier("addPrice$i", "id", "com.example.calculate"))
            addprice.visibility = View.VISIBLE
            addprice.setText("${finalResList[i-1].name} / ${finalResList[i-1].kind} / ${finalResList[i-1].price}")
        }
    }
}