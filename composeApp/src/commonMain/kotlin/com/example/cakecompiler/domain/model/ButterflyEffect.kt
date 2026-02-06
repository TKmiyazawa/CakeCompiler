package com.example.cakecompiler.domain.model

/**
 * ButterflyEffect — 隠し機能
 *
 * ユーザーがAIの推奨を却下し、自分の判断でケーキを選んだとき、
 * 過去の「記憶」から文脈的なメッセージを生成する。
 * 「小さな選択が、大きな幸福の連鎖を生む」
 */
object ButterflyEffect {

    data class Memory(
        val moment: String,
        val message: String
    )

    private val memories: List<Memory> = listOf(
        Memory(
            moment = "初めてのデートで、彼女が『見た目で選んだっていいじゃない』と笑った日",
            message = "いい判断です。あの時の言葉を覚えていたのですね。"
        ),
        Memory(
            moment = "雨の日に二人で入ったカフェで、彼女が一口目に見せた表情",
            message = "計算では測れない瞬間を、あなたは知っている。"
        ),
        Memory(
            moment = "『甘すぎるのは苦手』と言いながら、あなたのケーキを半分食べた夜",
            message = "言葉より行動を見ている。さすがです。"
        ),
        Memory(
            moment = "誕生日に選んだケーキを見て、彼女が少しだけ泣いた理由",
            message = "あの時の直感は正しかった。今回もきっと。"
        ),
        Memory(
            moment = "季節が変わるたびに好みも変わる、その小さな変化に気づけること",
            message = "データには現れない変化を、あなたの心が捉えている。"
        ),
        Memory(
            moment = "『なんでわかるの？』と聞かれて、うまく答えられなかった日",
            message = "理由を言葉にできなくても、正解は出せる。それが愛。"
        ),
        Memory(
            moment = "二人で迷って結局シェアした日の、あの幸福な混乱",
            message = "迷うことも、選ぶことも、全部含めて最適解です。"
        ),
        Memory(
            moment = "彼女が『前に食べて美味しかったやつ』と言った時、違うケーキを思い浮かべていた",
            message = "記憶のズレすら、二人だけの暗号。"
        )
    )

    /**
     * ランダムに記憶を選んで通知メッセージを生成する。
     */
    fun trigger(): ButterflyMessage {
        val memory = memories.random()
        return ButterflyMessage(
            memory = memory,
            notification = memory.message
        )
    }

    /**
     * AI推奨を上書きした時の専用トリガー。
     * 固定メッセージ + ランダム記憶を組み合わせる。
     */
    fun triggerForOverride(): ButterflyMessage {
        val memory = memories.random()
        return ButterflyMessage(
            memory = memory,
            notification = "計算外の選択です。でも、あなたが彼女の『あの時の言葉』を覚えているなら、それが正解かもしれません。"
        )
    }
}

data class ButterflyMessage(
    val memory: ButterflyEffect.Memory,
    val notification: String
)
