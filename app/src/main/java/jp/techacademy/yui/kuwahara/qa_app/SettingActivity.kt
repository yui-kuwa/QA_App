package jp.techacademy.yui.kuwahara.qa_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {

    private lateinit var mDataBaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Preferenceから表示名を取得してEditTextに反映させる
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        nameText.setText(name)

        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // UIの初期設定
        title = getString(R.string.settings_titile)

        //表示名変更ボタン
        changeButton.setOnClickListener { v ->
            // キーボードが出ていたら閉じる
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) { //ログインしているか確認
                // ログインしていない場合は何もしない
                //SnackBarでその旨を表示
                Snackbar.make(v, getString(R.string.no_login_user), Snackbar.LENGTH_LONG).show()
            } else {
                //ログインしている場合
                // 変更した表示名をFirebaseに保存する（クラウド上に保存）
                val name2 = nameText.text.toString()
                val userRef = mDataBaseReference.child(UsersPATH).child(user.uid)
                val data = HashMap<String, String>()
                data["name"] = name2
                userRef.setValue(data)

                // 変更した表示名をPreferenceに保存する（ローカル上に保存）
                val sp2 = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = sp2.edit()
                editor.putString(NameKEY, name2)//第二引数をnameからname2に変えた
                editor.commit()

                Snackbar.make(v, getString(R.string.change_disp_name), Snackbar.LENGTH_LONG).show()
            }
        }

        //ログアウトボタン
            logoutButton.setOnClickListener { v ->
                FirebaseAuth.getInstance().signOut()//ログアウト処理
                nameText.setText("")//Preferenceに空文字（”“)を保存
                //Snackbarでログアウト完了の旨を表示
                Snackbar.make(v, getString(R.string.logout_complete_message), Snackbar.LENGTH_LONG).show()
            }
    }
}