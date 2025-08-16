package com.xinqi.ui

import android.content.Context
import android.content.Intent
import com.xinqi.ui.character.CharacterInteractionActivity

object PageJumper {

    fun openCharacterPage(context: Context) {
        val intent = Intent(context, CharacterInteractionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

}