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
    l.contains("flashlight")||l.contains("flash light")||l.contains("torch")||l.contains("turn on light")||l.contains("turn off light") -> "FLASHLIGHT_ACTION"
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
  val raw=t.trim()
  val l=raw.lowercase(Locale.getDefault())
  try {
   // "open youtube <query>" launches YouTube and searches the requested phrase.
   if(l.startsWith("youtube search ") || l.startsWith("open youtube search ")){
    val query=raw.substringAfter("search ", "").trim()
    c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(query))))
    return "Searching YouTube for $query."
   }
   if(l.startsWith("open youtube ") && l.length>13){
    val query=raw.substringAfter("open youtube ", "").trim()
    if(query.isNotBlank()){
     c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(query))))
     return "Opening YouTube and searching for $query."
    }
   }

   val packages=when{
    l.contains("snapchat")->listOf("com.snapchat.android")
    l.contains("whatsapp")->listOf("com.whatsapp","com.whatsapp.w4b")
    l.contains("youtube")->listOf("com.google.android.youtube","com.google.android.apps.youtube.music")
    l.contains("chrome")->listOf("com.android.chrome")
    l.contains("telegram")->listOf("org.telegram.messenger")
    l.contains("instagram")->listOf("com.instagram.android")
    l.contains("facebook")->listOf("com.facebook.katana")
    l.contains("spotify")->listOf("com.spotify.music")
    l.contains("gmail")->listOf("com.google.android.gm")
    l.contains("maps")->listOf("com.google.android.apps.maps")
    else->emptyList()
   }

   if(l.contains("camera")){c.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE));return "Opening Camera."}
   if(l.contains("phone")||l.contains("dialer")){c.startActivity(Intent(Intent.ACTION_DIAL));return "Opening Phone."}
   if(l.contains("settings")){c.startActivity(Intent(Settings.ACTION_SETTINGS));return "Opening Settings."}

   for(pkg in packages){
    if(android.os.Build.VERSION.SDK_INT>=33){
     try{ c.startIntentSender(c.packageManager.getLaunchIntentSenderForPackage(pkg),null,0,0,0); return "Opening $raw." }catch(_:Exception){}
    }
    c.packageManager.getLaunchIntentForPackage(pkg)?.let{c.startActivity(it);return "Opening $raw."}
   }

   val wanted=when{
    l.contains("snapchat")->"snapchat";l.contains("whatsapp")->"whatsapp";l.contains("youtube")->"youtube"
    l.contains("chrome")->"chrome";l.contains("telegram")->"telegram";l.contains("instagram")->"instagram"
    l.contains("facebook")->"facebook";l.contains("spotify")->"spotify";l.contains("gmail")->"gmail";l.contains("maps")->"maps"
    else->raw.split(Regex("\\s+")).firstOrNull().orEmpty()
   }
   val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
   val hit=c.packageManager.queryIntentActivities(launcher,PackageManager.MATCH_ALL).firstOrNull{info->
    val label=info.loadLabel(c.packageManager).toString().lowercase(Locale.getDefault())
    label==wanted || label.contains(wanted)
   }
   if(hit!=null){
    val launch=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      .setComponent(android.content.ComponentName(hit.activityInfo.packageName,hit.activityInfo.name))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    c.startActivity(launch); return "Opening $raw."
   }
   return "$raw is not available on this device."
  }catch(_:Exception){ return "I couldn't open $raw." }
 }

 private fun dial(n0:String):String { val n=n0.filter{it.isDigit()||it=='+'}; if(n.isBlank())return "Please provide a phone number."; c.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+n))); return "Opening the dialer for "+n+"." }
 private fun sms(s:String):String { val m=Regex("([+0-9][+0-9 -]{5,})\\s+(.+)").find(s)?:return "Say: message 9876543210 hello."; val n=m.groupValues[1].replace(" ",""); val body=m.groupValues[2]; c.startActivity(Intent(Intent.ACTION_SENDTO).apply{data=Uri.parse("smsto:"+n);putExtra("sms_body",body)}); return "Opening SMS with your message ready." }
 private fun search(q:String):String { if(q.isBlank())return "Tell me what to search."; c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q)))); return "Searching for "+q+"." }
 private fun battery():String { val b=c.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); return "Battery is "+max(0,min(100,b))+"%." }
 private fun volume(d:Int):String { (c.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC,d,AudioManager.FLAG_SHOW_UI); return when(d){AudioManager.ADJUST_RAISE->"Volume increased.";AudioManager.ADJUST_LOWER->"Volume decreased.";else->"Media volume muted."} }
 private fun alarm(q:String):String { val m=Regex("(?:alarm|at)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",RegexOption.IGNORE_CASE).find(q)?:return "Say: alarm at 7:30 AM."; val h=m.groupValues[1].toInt(); val mn=m.groupValues[2].ifBlank{"0"}.toInt(); val ap=m.groupValues[3].lowercase(); val hh=if(ap=="pm"&&h<12)h+12 else if(ap=="am"&&h==12)0 else h; c.startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply{putExtra(android.provider.AlarmClock.EXTRA_HOUR,hh);putExtra(android.provider.AlarmClock.EXTRA_MINUTES,mn);putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"JARVIS alarm")}); return "Opening alarm setup for %02d:%02d.".format(hh,mn) }
}

class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener {
 private var tts:TextToSpeech?=null
 private var sr:SpeechRecognizer?=null
 private var pendingListen=false
 private var startupPermissions=true
 private var overlaySetupOpened=false
 private var listenResult:((String)->Unit)?=null
 private var listenStatus:((String)->Unit)?=null

 private val mic=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  if(pendingListen){
   if(granted){pendingListen=false;startListening(listenResult?:{},listenStatus?:{})}
   else{pendingListen=false;listenStatus?.invoke("Microphone permission denied.");Toast.makeText(this,"Microphone permission is required for Voice.",Toast.LENGTH_LONG).show()}
  }else if(startupPermissions){if(!granted)Toast.makeText(this,"Microphone skipped. Voice can be enabled later.",Toast.LENGTH_SHORT).show();window.decorView.post{requestStartupCamera()}}
 }
 private val camera=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  if(!granted)Toast.makeText(this,"Camera skipped. Flashlight can be enabled later.",Toast.LENGTH_SHORT).show()
  if(startupPermissions)window.decorView.post{requestStartupNotifications()}
 }
 private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){
  openOverlayIfNeeded()
 }

 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  tts=TextToSpeech(this,this)
  setContent{JarvisApp(::speak,::listen,{requestMic()},{requestCamera()},{requestNotifications()},{torch()},has(Manifest.permission.RECORD_AUDIO),has(Manifest.permission.CAMERA),has(Manifest.permission.POST_NOTIFICATIONS),Settings.canDrawOverlays(this),::overlay)}
  window.decorView.post{requestStartupPermission()}
 }
 private fun has(x:String)=ContextCompat.checkSelfPermission(this,x)==PackageManager.PERMISSION_GRANTED
 private fun requestMic(){if(!has(Manifest.permission.RECORD_AUDIO))mic.launch(Manifest.permission.RECORD_AUDIO)}
 private fun requestCamera(){if(!has(Manifest.permission.CAMERA))camera.launch(Manifest.permission.CAMERA)}
 private fun requestNotifications(){if(android.os.Build.VERSION.SDK_INT>=33&&!has(Manifest.permission.POST_NOTIFICATIONS))notifications.launch(Manifest.permission.POST_NOTIFICATIONS)}
 private fun requestStartupPermission(){
  if(!startupPermissions)return
  when{
   !has(Manifest.permission.RECORD_AUDIO)->mic.launch(Manifest.permission.RECORD_AUDIO)
   !has(Manifest.permission.CAMERA)->requestStartupCamera()
   android.os.Build.VERSION.SDK_INT>=33&&!has(Manifest.permission.POST_NOTIFICATIONS)->requestStartupNotifications()
   else->openOverlayIfNeeded()
  }
 }
 private fun openOverlayIfNeeded(){
  if(!startupPermissions)return
  if(android.os.Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this) && !overlaySetupOpened){
   overlaySetupOpened=true
   Toast.makeText(this,"Please allow JARVIS to display over other apps.",Toast.LENGTH_LONG).show()
   startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)))
  }else{
   startupPermissions=false
   Toast.makeText(this,"JARVIS setup complete.",Toast.LENGTH_SHORT).show()
  }
 }
 override fun onResume(){
  super.onResume()
  if(overlaySetupOpened){
   overlaySetupOpened=false
   if(startupPermissions)openOverlayIfNeeded()
  }
 }
 private fun requestStartupCamera(){if(!has(Manifest.permission.CAMERA))camera.launch(Manifest.permission.CAMERA)else requestStartupNotifications()}
 private fun requestStartupNotifications(){if(android.os.Build.VERSION.SDK_INT>=33&&!has(Manifest.permission.POST_NOTIFICATIONS))notifications.launch(Manifest.permission.POST_NOTIFICATIONS)else openOverlayIfNeeded()}
 private fun overlay(){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+packageName)))}
 private fun listen(result:(String)->Unit,status:(String)->Unit){
  listenResult=result;listenStatus=status
  if(!has(Manifest.permission.RECORD_AUDIO)){pendingListen=true;mic.launch(Manifest.permission.RECORD_AUDIO);status("Allow microphone permission…");return}
  startListening(result,status)
 }
 private fun startListening(result:(String)->Unit,status:(String)->Unit){
  if(!SpeechRecognizer.isRecognitionAvailable(this)){status("Voice recognition is unavailable on this phone.");return}
  sr?.cancel();sr?.destroy();sr=SpeechRecognizer.createSpeechRecognizer(this)
  sr!!.setRecognitionListener(object:RecognitionListener{
   override fun onReadyForSpeech(p:Bundle?){status("Listening…")}
   override fun onBeginningOfSpeech(){status("Hearing you…")}
   override fun onEndOfSpeech(){status("Processing…")}
   override fun onError(e:Int){status(when(e){SpeechRecognizer.ERROR_NO_MATCH->"I didn’t catch that. Try again.";SpeechRecognizer.ERROR_SPEECH_TIMEOUT->"I didn’t hear anything. Try again.";SpeechRecognizer.ERROR_AUDIO->"Microphone audio error. Check microphone access.";SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"Microphone permission is required.";SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"Voice service needs a network connection.";SpeechRecognizer.ERROR_RECOGNIZER_BUSY->"Voice service is busy. Try again.";else->"Voice error ($e). Tap Voice to retry."})}
   override fun onResults(b:Bundle?){val x=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull();if(x.isNullOrBlank())status("I didn’t catch that. Try again.")else result(x);status("Ready")}
   override fun onPartialResults(b:Bundle?){}
   override fun onBufferReceived(b:ByteArray?){}
   override fun onRmsChanged(v:Float){}
   override fun onEvent(t:Int,b:Bundle?){}
  })
  sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag());putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false)})
  status("Starting voice…")
 }
 private fun speak(s:String){tts?.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis")}
 private fun torch(){
  if(!has(Manifest.permission.CAMERA)){requestCamera();Toast.makeText(this,"Camera permission is needed for flashlight.",Toast.LENGTH_SHORT).show();return}
  try{val cm=getSystemService(android.hardware.camera2.CameraManager::class.java);val id=cm.cameraIdList.firstOrNull{cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true}?:return;val on=getPreferences(0).getBoolean("torch",false);cm.setTorchMode(id,!on);getPreferences(0).edit().putBoolean("torch",!on).apply()}catch(_:Exception){Toast.makeText(this,"Flashlight unavailable.",Toast.LENGTH_SHORT).show()}
 }
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS)tts?.language=Locale.getDefault()}
 override fun onDestroy(){sr?.destroy();tts?.stop();tts?.shutdown();super.onDestroy()}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun JarvisApp(
 speak:(String)->Unit, listen:((String)->Unit,(String)->Unit)->Unit,
 micReq:()->Unit,cameraReq:()->Unit,notifReq:()->Unit,torch:()->Unit,
 micOk:Boolean,cameraOk:Boolean,notifOk:Boolean,overlayOk:Boolean,overlay:()->Unit){
 val context=LocalContext.current;val engine=remember{JarvisEngine(context)};val msgs=remember{mutableStateListOf(Message(true,"JARVIS core online. Say a command or type one below."))};var input by remember{mutableStateOf("")};var status by remember{mutableStateOf("Ready")};var settings by remember{mutableStateOf(false)}
 fun run(q:String){if(q.isBlank())return;msgs.add(Message(false,q));val l=q.lowercase(Locale.getDefault());if(l.contains("flashlight")||l.contains("flash light")||l.contains("torch")||l.contains("turn on light")||l.contains("turn off light")){torch();val r="Flashlight toggled.";msgs.add(Message(true,r));speak(r)}else{val r=engine.handle(q);msgs.add(Message(true,r));speak(r)};input=""}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF7C5CFF),secondary=Color(0xFF39C8FF),background=Color(0xFF060811),surface=Color(0xFF101725))){
  Surface(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),color=Color(0xFF05070D)){Column(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=10.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("JARVIS",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("LOCAL INTELLIGENCE CORE",color=Color(0xFF55D6FF),style=MaterialTheme.typography.labelSmall)};Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF35E28B)));Spacer(Modifier.width(5.dp));Text(status.uppercase(),color=Color(0xFF9CEBC4),style=MaterialTheme.typography.labelSmall);IconButton({settings=true}){Icon(Icons.Default.Settings,"Settings")}}}
   Orb();LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){items(msgs){ChatBubble(it)}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){AssistChip(onClick={listen(::run){status=it}},label={Text("Voice")},leadingIcon={Icon(Icons.Default.Mic,null)});AssistChip(onClick=torch,label={Text("Flashlight")});AssistChip(onClick={ {run("what can you do")} },label={Text("Help")});AssistChip(onClick={ {run("what time is it")} },label={Text("Time")})}
   Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f),placeholder={Text("Talk to JARVIS…")},singleLine=true,shape=RoundedCornerShape(24.dp));IconButton({run(input)}){Icon(Icons.Default.Send,"Send")}}
  }}
  if(settings)ModalBottomSheet(onDismissRequest={settings=false}){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("JARVIS Capabilities",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Permissions are requested only when a feature needs them.");Perm("Microphone",micOk,micReq);Perm("Camera / Flashlight",cameraOk,cameraReq);Perm("Notifications",notifOk,notifReq);Perm("Floating assistant",overlayOk,overlay);Text("Core device commands do not need an AI API key. Android protected actions may show their own confirmation screen.");Spacer(Modifier.height(20.dp))}}
 }
}
@Composable fun Perm(name:String,ok:Boolean,go:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(name,fontWeight=FontWeight.SemiBold);Text(if(ok)"Enabled" else "Not enabled",color=if(ok)Color(0xFF55D99A)else Color(0xFFFFB454))};Button(onClick=go,enabled=!ok){Text(if(ok)"ON"else"Enable")}}}
@Composable fun Orb(){val t=rememberInfiniteTransition(label="orb");val p by t.animateFloat(.88f,1.12f,infiniteRepeatable(tween(1100),RepeatMode.Reverse),label="p");Box(Modifier.fillMaxWidth().height(155.dp),contentAlignment=Alignment.Center){Box(Modifier.size((145*p).dp).alpha(.16f).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF42C8FF),Color.Transparent))));Box(Modifier.size(92.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFB6F1FF),Color(0xFF1877E8),Color(0xFF071323)))));Text("J",color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)}}
@Composable fun ChatBubble(m:Message){Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.jarvis)Arrangement.Start else Arrangement.End){Surface(color=if(m.jarvis)Color(0xFF101A29)else Color(0xFF16425E),shape=RoundedCornerShape(20.dp)){Text(m.text,Modifier.padding(13.dp),color=Color.White)}}}
