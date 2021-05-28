package jp.techacademy.yui.kuwahara.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle    // ← 追加
import androidx.core.view.GravityCompat    // ← 追加
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.navigation.NavigationView    // ← 追加
import com.google.android.material.snackbar.Snackbar  // ← 追加
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_detail.view.*
import kotlinx.android.synthetic.main.app_bar_main.*    // ← 追加
import kotlinx.android.synthetic.main.app_bar_main.fab
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.listView

class MainActivity : AppCompatActivity() , NavigationView.OnNavigationItemSelectedListener {    // ← 修正

    private var mGenre = 0    // ← 追加

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private lateinit var mQuestionDetailActivity:QuestionDetailActivity
    private lateinit var mQuestion:Question

    // ログイン済みのユーザーを取得する
    val user = FirebaseAuth.getInstance().currentUser

    private var mGenreRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        //質問が追加された時に呼ばれるメソッド
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
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
            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList)//追加
            //ArrayListに追加
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        //質問に対して回答が投稿された時に呼ばれるメソッド
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            //取得した回答を設定
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
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
        setContentView(R.layout.activity_main)
        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        setSupportActionBar(toolbar)

        // fabにClickリスナーを登録
        fab.setOnClickListener { view ->
            // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
            if (mGenre == 0) { //初回起動時
                //最初の選択肢である「1:趣味」を表示する
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            } else {

            }
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていなければログイン画面に遷移させる
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        if(user == null){
            navigationView.menu.getItem(4).setVisible(false)
        }else{
            navigationView.menu.getItem(4).setVisible(true)
        }

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        //質問一覧画面でリストをタップしたらその質問の詳細画面に飛ぶリスナー
        listView.setOnItemClickListener{parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser///////追加

        // 1:趣味を既定の選択とする
        if(mGenre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }

        if(user == null){
            navigationView.menu.getItem(4).setVisible(false)
        }else{
            navigationView.menu.getItem(4).setVisible(true)
        }
    }

    //設定ボタン
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //右上のメニューから設定画面へ進む関数
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //ドロワーメニュー選択
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        // ログイン済みのユーザーを取得する
        val user2 = FirebaseAuth.getInstance().currentUser///////追加

        val id = item.itemId

        if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_compter) {
            toolbar.title = getString(R.string.menu_compter_label)
            mGenre = 4
        }

        //ログインしている場合、お気に入りもドロワーメニューで選択可能
        if (user2 != null) {
            if (id == R.id.nav_favorite) {//追加した　ドロワーメニューにお気に入り

                val intent = Intent(this@MainActivity, FavoriteActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            //リスナーはイベントごとに複数回呼び出されるため、そのリスナーを完全に削除するには同じ回数だけデタッチする必要がある
            mGenreRef!!.removeEventListener(mEventListener)
        }

        //contentの中のジャンルを指定
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        mGenreRef!!.addChildEventListener(mEventListener)

        return true
    }
}