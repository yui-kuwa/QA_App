package jp.techacademy.yui.kuwahara.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    //Firebase関連
    private lateinit var mAuth: FirebaseAuth
    //アカウント作成処理用
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    //ログイン処理用
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    //データベースへの読み書き
    private lateinit var mDataBaseReference: DatabaseReference

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //データベースへのリファレンスを取得
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuthのオブジェクト(インスタンス)を取得する
        mAuth = FirebaseAuth.getInstance()

        // アカウント作成処理のリスナー
        mCreateAccountListener = OnCompleteListener { task ->//引数で渡ってきたTaskクラスのisSuccessfulメソッドを使って、成功したかどうかを確認
            if (task.isSuccessful) {
                // 成功した場合
                // ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)//loginメソッド
            } else {

                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                //Snackbarでエラーの旨を表示
                Snackbar.make(
                    view,
                    getString(R.string.create_account_failure_message),
                    Snackbar.LENGTH_LONG
                ).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                val user = mAuth.currentUser//////////////
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)////////////////

                //mIsCreateAccountを使って、アカウント作成ボタンを押してからのログイン処理の場合
                if (mIsCreateAccount) {
                    // アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()

                    val data = HashMap<String, String>()
                    data["name"] = name
                    //setValue メソッドでDatabaseReferenceが指し示すKeyにValueを保存する
                    userRef.setValue(data)

                    // 表示名をPreferenceに保存する
                    saveName(name)
                } else {//すぐにログインする場合の処理
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {////////////////
                        override fun onDataChange(snapshot: DataSnapshot) {//////////////////
                            val data = snapshot.value as Map<*, *>?/////////////
                            saveName(data!!["name"] as String)
                        }

                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // タイトルの設定(タイトルバーのタイトルを変更)
        title = getString(R.string.login_title)

        //アカウント作成ボタンのリスナー
        createButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // ログイン時に表示名を保存するようにフラグを立てる
                mIsCreateAccount = true

                //アカウント作成処理を開始
                createAccount(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }

        //ログインボタンのリスナー
        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                //ログイン処理を開始
                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    //アカウント作成
    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // アカウントを作成する　　　addOnCompleteListenerメソッドを呼び出してリスナーを設定
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    //ログイン
    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // ログインする　　　　　addOnCompleteListenerメソッドを呼び出してリスナーを設定
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    //引数で受け取った表示名をPreferenceに保存する
    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        //保存処理を反映
        editor.commit()
    }

}