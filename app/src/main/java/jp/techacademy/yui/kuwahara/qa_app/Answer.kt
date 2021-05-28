package jp.techacademy.yui.kuwahara.qa_app

import java.io.Serializable

//質問の回答のモデルクラス
class Answer(val body: String, val name: String, val uid: String, val answerUid: String) : Serializable