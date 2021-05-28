package jp.techacademy.yui.kuwahara.qa_app

import java.io.Serializable
import java.util.ArrayList

//タイトル、質問本文、質問者の名前、質問者のUID、質問のUID、質問のジャンル、取得した画像をbyte型の配列にしたもの、取得した質問のモデルクラスであるAnswerのArrayList、お気に入り情報
class Question (val title: String, val body: String, val name: String, val uid: String, val questionUid: String, val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>) : Serializable {
    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}