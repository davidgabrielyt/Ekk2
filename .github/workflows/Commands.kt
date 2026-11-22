package br.com.ekko

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Commands {
    private val pt = Locale("pt", "BR")

    private fun go(c: Context, i: Intent) {
        try { c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
    }
    private fun view(c: Context, url: String) = go(c, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun phoneOf(c: Context, name: String): String? {
        val k = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        c.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "$k LIKE ?", arrayOf("%$name%"), null
        )?.use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    private fun media(c: Context, key: Int) {
        val a = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        a.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        a.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
    }

    fun run(c: Context, raw: String, say: (String) -> Unit) {
        val l = raw.lowercase(pt).trim().trimEnd('.', '?', '!')

        if (Regex("que horas|horas são").containsMatchIn(l)) {
            say("Agora são " + SimpleDateFormat("HH:mm", pt).format(Date())); return
        }
        if (Regex("que dia|data de hoje").containsMatchIn(l)) {
            say("Hoje é " + SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", pt).format(Date())); return
        }
        if (Regex("timer|temporizador|cronômetro").containsMatchIn(l)) {
            val s = Regex("(\\d+) ?(hora|minuto|segundo)").findAll(l).sumOf {
                val n = it.groupValues[1].toInt()
                when (it.groupValues[2]) { "hora" -> n * 3600; "minuto" -> n * 60; else -> n }
            }
            if (s > 0) {
                go(c, Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, s)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, "Ekko").putExtra(AlarmClock.EXTRA_SKIP_UI, true))
                say("Timer de ${s / 60} minutos e ${s % 60} segundos iniciado."); return
            }
        }
        Regex("alarme.*?(\\d{1,2})(?:[:h ](\\d{2}))?").find(l)?.let { m ->
            val h = m.groupValues[1].toInt(); val min = m.groupValues[2].ifEmpty { "0" }.toInt()
            go(c, Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, h)
                .putExtra(AlarmClock.EXTRA_MINUTES, min).putExtra(AlarmClock.EXTRA_SKIP_UI, true))
            say("Alarme definido para %d:%02d.".format(h, min)); return
        }
        if (l.contains("lanterna")) {
            val on = !l.contains("desligar") && !l.contains("apagar")
            try {
                val cm = c.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cm.setTorchMode(cm.cameraIdList[0], on)
                say(if (on) "Lanterna ligada." else "Lanterna desligada.")
            } catch (e: Exception) { say("Não consegui usar a lanterna.") }
            return
        }
        if (Regex("aumentar o volume|mais alto|volume para cima").containsMatchIn(l) ||
            Regex("diminuir o volume|abaixar o volume|mais baixo").containsMatchIn(l)) {
            val up = Regex("aumentar|mais alto|para cima").containsMatchIn(l)
            (c.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            say("Pronto."); return
        }
        if (Regex("pausar|pause").containsMatchIn(l)) { media(c, KeyEvent.KEYCODE_MEDIA_PAUSE); say("Pausado."); return }
        if (Regex("continuar|retomar|dar play").containsMatchIn(l)) { media(c, KeyEvent.KEYCODE_MEDIA_PLAY); say("Tocando."); return }
        if (Regex("próxima|pular").containsMatchIn(l)) { media(c, KeyEvent.KEYCODE_MEDIA_NEXT); say("Próxima."); return }
        if (l.contains("anterior")) { media(c, KeyEvent.KEYCODE_MEDIA_PREVIOUS); say("Anterior."); return }

        Regex("(?:ligar|chamar|liga) (?:para |pra )?(.+)").find(l)?.let { m ->
            val q = m.groupValues[1]; val digits = q.filter { it.isDigit() }
            val num = if (digits.length >= 8) digits else phoneOf(c, q)
            if (num != null) { go(c, Intent(Intent.ACTION_CALL, Uri.parse("tel:$num"))); say("Ligando para $q."); return }
            say("Não encontrei $q nos contatos."); return
        }
        Regex("^(?:abrir|abra|iniciar) (?:o |a )?(.+)").find(l)?.let { m ->
            val n = m.groupValues[1]; val pm = c.packageManager
            val app = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .firstOrNull { it.loadLabel(pm).toString().lowercase(pt).contains(n) }
            val li = app?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
            if (li != null) { go(c, li); say("Abrindo $n."); return }
            say("Não achei o app $n."); return
        }
        Regex("(?:tocar|toque|colocar|coloca) (.+)").find(l)?.let { m ->
            view(c, "https://www.youtube.com/results?search_query=" + Uri.encode(m.groupValues[1]))
            say("Procurando ${m.groupValues[1]}."); return
        }
        Regex("(?:como chegar|rota|navegar) (?:para |ao |a )?(.+)").find(l)?.let { m ->
            view(c, "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(m.groupValues[1]))
            say("Traçando a rota."); return
        }
        Regex("(?:pesquisar|buscar|procurar) (?:por |sobre )?(.+)").find(l)?.let { m ->
            view(c, "https://www.google.com/search?q=" + Uri.encode(m.groupValues[1]))
            say("Pesquisando."); return
        }
        ai(c, raw, say)
    }

    private fun ai(c: Context, q: String, say: (String) -> Unit) {
        val key = BuildConfig.ANTHROPIC_KEY
        if (key.isBlank()) { view(c, "https://www.google.com/search?q=" + Uri.encode(q)); say("Pesquisando."); return }
        Thread {
            try {
                val body = JSONObject().put("model", "claude-sonnet-4-6").put("max_tokens", 300)
                    .put("system", "Você é a Ekko, assistente de voz em português do Brasil. Responda em no máximo 3 frases curtas, sem markdown.")
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", q)))
                val con = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
                con.requestMethod = "POST"; con.doOutput = true
                con.setRequestProperty("x-api-key", key)
                con.setRequestProperty("anthropic-version", "2023-06-01")
                con.setRequestProperty("content-type", "application/json")
                con.outputStream.use { it.write(body.toString().toByteArray()) }
                val r = con.inputStream.bufferedReader().readText()
                say(JSONObject(r).getJSONArray("content").getJSONObject(0).getString("text"))
            } catch (e: Exception) { say("Não consegui falar com a IA agora.") }
        }.start()
    }
}
