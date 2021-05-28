package jp.techacademy.yui.kuwahara.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.widget.ListView
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_favorite.*
import kotlinx.android.synthetic.main.content_main.*

class FavoriteActivity : AppCompatActivity() /*, NavigationView.OnNavigationItemSelectedListener */{

    private lateinit var mQuestion: Question
    private lateinit var mFavoriteQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    // ログイン済みのユーザーを取得する
    val user = FirebaseAuth.getInstance().currentUser

    //データベースへの読み書き////////////////////////////////////////
    private var mDataBaseReference = FirebaseDatabase.getInstance().reference//インスタンス

    private val mEventListener = object : ChildEventListener {
        //onChildAddedはお気に入りにデータがあれば呼び出される(お気に入り一覧を取得)
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

//          val mapQ = dataSnapshot.value as Map<String, String>
            Log.d("kotlintest:key",dataSnapshot.key.toString())
//            Log.d("kotlintest",s.toString())

            //質問を取得
            val questionUid = dataSnapshot.key!!

            val mapG = dataSnapshot.value as Map<String, String>

            val questionRef = mDataBaseReference!!.child(ContentsPATH)

            //ジャンルを取得
            val genre = mapG["genre"]!!

            //取得したジャンルを利用して、質問→ジャンルを辿って質問内容を全取得
            questionRef.child(genre).child(questionUid).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = snapshot.value as Map<String, String>
                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }

                    //Answerクラスを作成
                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            //ArrayListに追加
                            answerArrayList.add(answer)
                        }
                    }

                    //Questionクラスを作成
                    val question = Question(title, body, name, uid, questionUid,
                        genre.toInt(), bytes, answerArrayList)//追加
                    //ArrayListに追加
                    mFavoriteQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(firebaseError: DatabaseError) {}
            })
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        toolbar.title = getString(R.string.menu_favorite_label)

        //val favoriteRef = mDataBaseReference!!.child(FavoritesPATH).child(user!!.uid)

        // ListViewの準備
        //mAdapterはQuestionListAdapterクラスを代入
        mAdapter = QuestionsListAdapter(this)

        //Questionクラスの配列リストを初期化
        mFavoriteQuestionArrayList = ArrayList<Question>()

/*        mFavoriteQuestionArrayList.clear()

        //お気に入りの一覧を取得するリスナーを呼ぶ
        favoriteRef.addChildEventListener(mEventListener)

       //お気に入りのリストをアダプターに
        mAdapter.setQuestionArrayList(mFavoriteQuestionArrayList)

        //リストビューにアダプターを付ける
        list_View.adapter = mAdapter*/

        //質問一覧画面でリストをタップしたらその質問の詳細画面に飛ぶリスナー
        list_View.setOnItemClickListener{parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mFavoriteQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        val favoriteRef = mDataBaseReference!!.child(FavoritesPATH).child(user!!.uid)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mFavoriteQuestionArrayList.clear()

        //お気に入りの一覧を取得するリスナーを呼ぶ
        favoriteRef.addChildEventListener(mEventListener)

        mAdapter.setQuestionArrayList(mFavoriteQuestionArrayList)
        list_View.adapter = mAdapter

        mAdapter.notifyDataSetChanged()
    }
}