package com.example.cakecompiler.presentation.model

/**
 * "Whimsical Status Strings" - 遊び心のある状態表示
 *
 * システムメッセージを温かく遊び心のあるトーンに変換する。
 * 「正解を出すツール」ではなく「二人で楽しむためのデバイス」として振る舞う。
 */
object WhimsicalStrings {

    /**
     * 読込中のメッセージ
     */
    object Loading {
        val messages = listOf(
            "彼女の過去の笑顔をコンパイル中...",
            "甘い記憶をメモリにロード中...",
            "ふたりの好みを量子もつれさせています...",
            "トキメキのベクトルを解析中...",
            "幸福関数を最適化しています..."
        )

        fun random(): String = messages.random()

        val primary: String = messages.first()
    }

    /**
     * 解析完了のメッセージ
     */
    object AnalysisComplete {
        val messages = listOf(
            "最高の一口を計算し終えました。あとは君の勇気次第！",
            "準備完了！運命のケーキが待っています。",
            "計算終了。でも最後に決めるのは、君の心。",
            "幸福度 H(x) を最大化する解を発見！",
            "彼女の笑顔の確率、算出しました。"
        )

        fun random(): String = messages.random()

        val primary: String = messages.first()
    }

    /**
     * 異常検知（Serendipity）のメッセージ
     */
    object SerendipityDetected {
        val messages = listOf(
            "おっと！計算外のトキメキを検出。新しい彼女の発見ですね？",
            "予測外の選択...これは、新しい一面の発見かも！",
            "統計からの逸脱を検出。恋の方程式が更新されます。",
            "サプライズ検知！彼女の知らなかった好みかもしれません。",
            "想定外のデータポイント。これも愛の形です。"
        )

        fun random(): String = messages.random()

        val primary: String = messages.first()
    }

    /**
     * オーバーライド時のメッセージ
     */
    object Override {
        val aiDismissed = listOf(
            "AIの提案を優しく見送りました...",
            "計算より直感を信じるあなた、素敵です。",
            "アルゴリズムより愛を選びましたね。"
        )

        val userConfirmed = listOf(
            "君の決断が、新しい公式になる。",
            "これが、君だけの正解。",
            "愛は計算を超える。確定しました。",
            "The Final Bit: あなたの意思で決まりました。"
        )

        fun randomDismissed(): String = aiDismissed.random()
        fun randomConfirmed(): String = userConfirmed.random()
    }

    /**
     * シェイクで Serendipity モードのメッセージ
     */
    object ShakeToSerendipity {
        val activated = listOf(
            "冒険モード起動！いつもと違う選択肢を探します...",
            "シェイク検知！計算外の運命を召喚中...",
            "規則性からの脱出。今日は意外な出会いを。"
        )

        val suggestion = listOf(
            "統計が絶対選ばないこのケーキ、試してみる？",
            "普段とは真逆のベクトル。新しい扉を開いてみませんか？",
            "計算からの解放。この偶然に賭けてみる？"
        )

        fun randomActivated(): String = activated.random()
        fun randomSuggestion(): String = suggestion.random()
    }

    /**
     * 幸福度に応じたメッセージ
     */
    object HappinessLevel {
        fun getMessage(score: Double): String = when {
            score >= 0.9 -> "✨ 完璧なマッチング！ふたりの笑顔が見えます。"
            score >= 0.8 -> "💕 とても良い選択。彼女も喜ぶはず！"
            score >= 0.7 -> "😊 なかなかの相性。試してみる価値あり。"
            score >= 0.6 -> "🤔 悪くないけど、もっといいのがあるかも？"
            score >= 0.5 -> "💭 微妙なライン...でも冒険もいいかも。"
            else -> "🎲 挑戦的な選択。新しい発見があるかもしれません。"
        }
    }

    /**
     * パートナー優先を示すメッセージ
     */
    object PartnerFirst {
        val reminders = listOf(
            "彼女の笑顔が、最高のデザート。",
            "「あなたが選んでくれたから美味しい」そう言わせよう。",
            "自分より彼女。その気持ち、ちゃんと計算に入れてます。"
        )

        fun random(): String = reminders.random()
    }

    /**
     * エラー時のメッセージ（それでも温かく）
     */
    object Error {
        val messages = listOf(
            "あれ？ちょっと計算が迷子になりました。もう一度試してみて？",
            "一時的な接続の問題。でも大丈夫、もう一度やってみよう。",
            "想定外のエラー。でも愛にエラーはないから大丈夫。"
        )

        fun random(): String = messages.random()
    }
}

/**
 * 状態に応じた表示文字列を生成
 */
data class StatusMessage(
    val text: String,
    val type: StatusType,
    val emoji: String? = null
) {
    enum class StatusType {
        LOADING,
        SUCCESS,
        SERENDIPITY,
        OVERRIDE,
        ERROR,
        INFO
    }

    companion object {
        fun loading(): StatusMessage = StatusMessage(
            text = WhimsicalStrings.Loading.random(),
            type = StatusType.LOADING
        )

        fun complete(): StatusMessage = StatusMessage(
            text = WhimsicalStrings.AnalysisComplete.random(),
            type = StatusType.SUCCESS,
            emoji = "🎂"
        )

        fun serendipity(): StatusMessage = StatusMessage(
            text = WhimsicalStrings.SerendipityDetected.random(),
            type = StatusType.SERENDIPITY,
            emoji = "✨"
        )

        fun override(): StatusMessage = StatusMessage(
            text = WhimsicalStrings.Override.randomConfirmed(),
            type = StatusType.OVERRIDE,
            emoji = "💫"
        )

        fun overriding(): StatusMessage = StatusMessage(
            text = "AIの予測を上書きしています...",
            type = StatusType.OVERRIDE,
            emoji = "⏳"
        )

        fun error(): StatusMessage = StatusMessage(
            text = WhimsicalStrings.Error.random(),
            type = StatusType.ERROR
        )
    }
}
