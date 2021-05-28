package jp.techacademy.yui.kuwahara.qa_app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    //Firebase関連
    private lateinit var mAuth: FirebaseAuth

    //ログイン処理用
    private lateinit var mFavoriteListener: OnCompleteListener<AuthResult>

    // ログイン済みのユーザーを取得する
    val user = FirebaseAuth.getInstance().currentUser

    //データベースへの読み書き////////////////////////////////////////
    private lateinit var mDataBaseReference: DatabaseReference
    //////////////////////////////////////////////////////////////

    // お気に入り状態
    var isFavorite:Boolean = false

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    private val mEventListener2 = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            Log.d("kotlintest",dataSnapshot.toString())
            if(dataSnapshot.value != null) {
                isFavorite = true
                changeFavoriteImageView(isFavorite)
            }else{
                isFavorite = false
                changeFavoriteImageView(isFavorite)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
        }
    }

    val data = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        mDataBaseReference = FirebaseDatabase.getInstance().reference//インスタンス

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

/*        // ログイン済みのユーザーを取得する     外で定義
        val user = FirebaseAuth.getInstance().currentUser*/

        fab.setOnClickListener {

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        if(user != null){
            val favoriteRef = mDataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)

            favoriteRef.addValueEventListener(mEventListener2)
            //画像を変更
            changeFavoriteImageView(isFavorite)

            //先にユーザがログインしてるかの情報を得てから、お気に入りボタンをクリックできるようにするかできないようにするか（できないようにするには）
            favoriteImageView.setOnClickListener {
                //データを保存する場所を指定

                data["genre"] = mQuestion.genre.toString()

                    if (isFavorite == false) {//登録
                        isFavorite = true
                        //画像の設定
                        favoriteImageView.setImageResource(R.drawable.ic_star)

                        //pushでユニークなIDを振る、setValueでコンソールに登録
                        favoriteRef.setValue(data)

                    } else {//解除
                        isFavorite = false
                        //画像の設定
                        favoriteImageView.setImageResource(R.drawable.ic_star_border)

                        favoriteRef.removeValue()
                    }
            }
        }else{
            favoriteImageView.visibility = View.INVISIBLE//imageViewを表示しない
            favoriteImageView.isClickable = false
            favoriteImageView.isVisible = false
        }

    }

    override fun onResume() {
        super.onResume()

        if(user != null){
            changeFavoriteImageView(isFavorite)
        }
    }

    fun changeFavoriteImageView(favorite:Boolean){
        favoriteImageView.setImageResource(
            if (favorite) R.drawable.ic_star
            else R.drawable.ic_star_border
        )
    }
}