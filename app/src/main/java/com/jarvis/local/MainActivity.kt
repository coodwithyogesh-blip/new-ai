package com.jarvis.local

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class Message(val jarvis:Boolean,val text:String)

class JarvisEngine(private val c:Context) {
 private val p=c.getSharedPreferences("jarvis_memory",Context.MODE_PRIVATE)
 fun handle(q0:String):String {
  val q=q0.trim(); val l=q.lowercase(Locale.getDefault())
  if(q.isBlank()) return "Please say something."
  return try {
   when {
    l.matches(Regex(".*\\b(hello|hi|hey|namaste)\\b.*")) -> "Hello. JARVIS core is ready."
    l.contains("who are you")||l.contains("what are you") -> "I am JARVIS, a local-first Android assistant. No AI API key is required for these device commands."
    l=="time"||l.contains("what time") -> "The current time is "+SimpleDateFormat("hh:mm a",Locale.getDefault()).format(Date())
    l.contains("battery") -> battery()
    l.startsWith("remember ") -> { val m=q.substringAfter("remember ").trim(); p.edit().putString("memory",m).apply(); "Remembered: "+m }
    l.contains("what do you remember")||l.contains("my memory") -> "I remember: "+(p.getString("memory",null)?:"nothing yet")
    l.contains("forget memory") -> { p.edit().remove("memory").apply(); "Memory cleared." }
    l.startsWith("open ") -> open(q.substringAfter("open ").trim())
    l.startsWith("call ")||l.startsWith("dial ") -> dial(q.substringAfter(" ").trim())
    l.startsWith("message ")||l.startsWith("sms ")||l.startsWith("text ") -> sms(q.substringAfter(" ").trim())
    l.startsWith("search ")||l.startsWith("google ") -> search(q.substringAfter(" ").trim())
    l.contains("wifi") -> { c.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)); "Opening Wi-Fi settings." }
    l.contains("bluetooth") -> { c.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); "Opening Bluetooth settings." }
    l.contains("settings") -> { c.startActivity(Intent(Settings.ACTION_SETTINGS)); "Opening Android Settings." }
    l.contains("volume up")||l.contains("increase volume") -> volume(AudioManager.ADJUST_RAISE)
    l.contains("volume down")||l.contains("decrease volume") -> volume(AudioManager.ADJUST_LOWER)
    l.contains("mute") -> volume(AudioManager.ADJUST_MUTE)
    l.contains("alarm") -> alarm(q)
    l.contains("help")||l.contains("what can you do") -> "Try: open WhatsApp, open YouTube, call 123, message 123 hello, search cats, open Wi-Fi, volume up, remember my note, or what do you remember."
    else -> "I understood the command, but I don't have a safe action for it yet."
   }
  } catch(e:Exception) { "Action failed safely: "+(e.message?:"unknown error") }
 }
 private fun open(t:String):String {
  val l=t.lowercase(Locale.getDefault())
  try {
   val pkg=when { l.contains("whatsapp")->"com.whatsapp"; l.contains("youtube")->"com.google.android.youtube"; l.contains("chrome")->"com.android.chrome"; l.contains("telegram")->"org.telegram.messenger"; l.contains("instagram")->"com.instagram.android"; else->null }
   if(pkg!=null){ val i=c.packageManager.getLaunchIntentForPackage(pkg); if(i!=null){c.startActivity(i);return "Opening "+t+"."}; return t+" is not installed." }
   when { l.contains("camera")->c.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)); l.contains("phone")||l.contains("dialer")->c.startActivity(Intent(Intent.ACTION_DIAL)); l.contains("settings")->c.startActivity(Intent(Settings.ACTION_SETTINGS)); else->return "I don't know how to open "+t+" yet." }
   return "Opening "+t+"."
  }catch(e:Exception){return "I couldn't open "+t+"."}
 }
 private fun dial(n0:String):String { val n=n0.filter{it.isDigit()||it=='+'}; if(n.isBlank())return "Please provide a phone number."; c.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+n))); return "Opening the dialer for "+n+"." }
 private fun sms(s:String):String { val m=Regex("([+0-9][+0-9 -]{5,})\\s+(.+)").find(s)?:return "Say: message 9876543210 hello."; val n=m.groupValues[1].replace(" ",""); val body=m.groupValues[2]; c.startActivity(Intent(Intent.ACTION_SENDTO).apply{data=Uri.parse("smsto:"+n);putExtra("sms_body",body)}); return "Opening SMS with your message ready." }
 private fun search(q:String):String { if(q.isBlank())return "Tell me what to search."; c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q)))); return "Searching for "+q+"." }
 private fun battery():String { val b=c.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); return "Battery is "+max(0,min(100,b))+"%." }
 private fun volume(d:Int):String { (c.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC,d,AudioManager.FLAG_SHOW_UI); return when(d){AudioManager.ADJUST_RAISE->"Volume increased.";AudioManager.ADJUST_LOWER->"Volume decreased.";else->"Media volume muted."} }
 private fun alarm(q:String):String { val m=Regex("(?:alarm|at)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",RegexOption.IGNORE_CASE).find(q)?:return "Say: alarm at 7:30 AM."; val h=m.groupValues[1].toInt(); val mn=m.groupValues[2].ifBlank{"0"}.toInt(); val ap=m.groupValues[3].lowercase(); val hh=if(ap=="pm"&&h<12)h+12 else if(ap=="am"&&h==12)0 else h; c.startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply{putExtra(android.provider.AlarmClock.EXTRA_HOUR,hh);putExtra(android.provider.AlarmClock.EXTRA_MINUTES,mn);putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"JARVIS alarm")}); return "Opening alarm setup for %02d:%02d.".format(hh,mn) }
}

class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener {
 private var tts:TextToSpeech?=null; private var sr:SpeechRecognizer?=null
 private val mic=registerForActivityResult(ActivityResultContracts.RequestPermission()){ }
 private val camera=registerForActivityResult(ActivityResultContracts.RequestPermission()){ }
 private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){ }
 override fun onCreate(b:Bundle?){\n  super.onCreate(b)\n  tts=TextToSpeech(this,this)\n  setContent{JarvisApp(::speak,::listen,{requestMic()},{requestCamera()},{requestNotifications()},{torch()},has(Manifest.permission.RECORD_AUDIO),has(Manifest.permission.CAMERA),::overlay)}\n  if(!has(Manifest.permission.RECORD_AUDIO)) requestMic()\n }
 private fun has(x:String)=ContextCompat.checkSelfPermission(this,x)==PackageManager.PERMISSION_GRANTED
 private fun requestMic(){if(!has(Manifest.permission.RECORD_AUDIO))mic.launch(Manifest.permission.RECORD_AUDIO)}
 private fun requestCamera(){if(!has(Manifest.permission.CAMERA))camera.launch(Manifest.permission.CAMERA)}
 private fun requestNotifications(){if(android.os.Build.VERSION.SDK_INT>=33&&!has(Manifest.permission.POST_NOTIFICATIONS))notifications.launch(Manifest.permission.POST_NOTIFICATIONS)}
 private fun overlay(){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)))}
 private fun listen(result:(String)->Unit,status:(String)->Unit){
  if(!has(Manifest.permission.RECORD_AUDIO)){requestMic();status("Microphone permission required.");return}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){status("Speech recognition unavailable.");return}
  sr?.destroy(); sr=SpeechRecognizer.createSpeechRecognizer(this)
  sr!!.setRecognitionListener(object:RecognitionListener{
   override fun onReadyForSpeech(p:Bundle?){status("Listening…")}; override fun onBeginningOfSpeech(){status("Listening…")}; override fun onEndOfSpeech(){status("Processing…")}
   override fun onError(e:Int){status("Voice error. Tap Voice and try again.")}; override fun onResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(result);status("Ready")}
   override fun onPartialResults(b:Bundle?){ }; override fun onBufferReceived(b:ByteArray?){ }; override fun onRmsChanged(v:Float){}; override fun onEvent(t:Int,b:Bundle?){ }
  })
  sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)})
 }
 private fun speak(s:String){tts?.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis")}
 private fun torch(){
  if(!has(Manifest.permission.CAMERA)){requestCamera();Toast.makeText(this,"Camera permission is needed for flashlight.",Toast.LENGTH_SHORT).show();return}
  try{val cm=getSystemService(android.hardware.camera2.CameraManager::class.java);val id=cm.cameraIdList.firstOrNull{cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true}?:return;val on=getPreferences(0).getBoolean("torch",false);cm.setTorchMode(id,!on);getPreferences(0).edit().putBoolean("torch",!on).apply()}catch(_:Exception){Toast.makeText(this,"Flashlight unavailable.",Toast.LENGTH_SHORT).show()}
 }
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS)tts?.language=Locale.getDefault()}
 override fun onDestroy(){sr?.destroy();tts?.stop();tts?.shutdown();super.onDestroy()}
}

@Composable fun JarvisApp(
 speak:(String)->Unit, listen:((String)->Unit,(String)->Unit)->Unit,
 micReq:()->Unit,cameraReq:()->Unit,notifReq:()->Unit,torch:()->Unit,
 micOk:Boolean,cameraOk:Boolean,overlay:()->Unit){
 val context=LocalContext.current;val engine=remember{JarvisEngine(context)};val msgs=remember{mutableStateListOf(Message(true,"JARVIS core online. Say a command or type one below."))};var input by remember{mutableStateOf("")};var status by remember{mutableStateOf("Ready")};var settings by remember{mutableStateOf(false)}
 fun run(q:String){if(q.isBlank())return;msgs.add(Message(false,q));val r=engine.handle(q);msgs.add(Message(true,r));speak(r);input=""}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF7C5CFF),secondary=Color(0xFF39C8FF),background=Color(0xFF060811),surface=Color(0xFF101725))){
  Surface(Modifier.fillMaxSize(),color=Color(0xFF060811)){Column(Modifier.fillMaxSize().padding(16.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("JARVIS",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("LOCAL INTELLIGENCE CORE",color=Color(0xFF55D6FF),style=MaterialTheme.typography.labelSmall)};Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF35E28B)));Spacer(Modifier.width(5.dp));Text(status.uppercase(),color=Color(0xFF9CEBC4),style=MaterialTheme.typography.labelSmall);IconButton({settings=true}){Icon(Icons.Default.Settings,"Settings")}}}
   Orb();LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){items(msgs){ChatBubble(it)}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){AssistChip(onClick={listen(::run){status=it}},label={Text("Voice")},leadingIcon={Icon(Icons.Default.Mic,null)});AssistChip(onClick=torch,label={Text("Flashlight")});AssistChip(onClick={ {run("what can you do")} },label={Text("Help")});AssistChip(onClick={ {run("what time is it")} },label={Text("Time")})}
   Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f),placeholder={Text("Talk to JARVIS…")},singleLine=true,shape=RoundedCornerShape(24.dp));IconButton({run(input)}){Icon(Icons.Default.Send,"Send")}}
  }}
  if(settings)ModalBottomSheet(onDismissRequest={settings=false}){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("JARVIS Capabilities",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Permissions are requested only when a feature needs them.");Perm("Microphone",micOk,micReq);Perm("Camera / Flashlight",cameraOk,cameraReq);Perm("Notifications",false,notifReq);Perm("Floating assistant",Settings.canDrawOverlays(context),overlay);Text("Core device commands do not need an AI API key. Android protected actions may show their own confirmation screen.");Spacer(Modifier.height(20.dp))}}
 }
}
@Composable fun Perm(name:String,ok:Boolean,go:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(name,fontWeight=FontWeight.SemiBold);Text(if(ok)"Enabled" else "Not enabled",color=if(ok)Color(0xFF55D99A)else Color(0xFFFFB454))};Button(onClick=go,enabled=!ok){Text(if(ok)"ON"else"Enable")}}}
@Composable fun Orb(){val t=rememberInfiniteTransition(label="orb");val p by t.animateFloat(.88f,1.12f,infiniteRepeatable(tween(1100),RepeatMode.Reverse),label="p");Box(Modifier.fillMaxWidth().height(180.dp),contentAlignment=Alignment.Center){Box(Modifier.size((145*p).dp).alpha(.16f).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF42C8FF),Color.Transparent))));Box(Modifier.size(92.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFB6F1FF),Color(0xFF1877E8),Color(0xFF071323)))));Text("J",color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)}}
@Composable fun ChatBubble(m:Message){Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.jarvis)Arrangement.Start else Arrangement.End){Surface(color=if(m.jarvis)Color(0xFF101A29)else Color(0xFF16425E),shape=RoundedCornerShape(20.dp)){Text(m.text,Modifier.padding(13.dp),color=Color.White)}}}
