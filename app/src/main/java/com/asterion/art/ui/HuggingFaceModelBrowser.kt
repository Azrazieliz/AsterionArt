package com.asterion.art.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asterion.art.models.*
import com.asterion.art.work.HuggingFaceDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Search real Hub repositories, inspect exact files, select/download in-app. */
@Composable
fun HuggingFaceModelBrowser(selectedFamily:String="auto") {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val hub=remember{HuggingFaceHub()}
    var query by remember{mutableStateOf("Qwen3 GGUF")}
    var searching by remember{mutableStateOf(false)}
    var repositories by remember{mutableStateOf<List<HubRepository>>(emptyList())}
    var chosen by remember{mutableStateOf<HubRepository?>(null)}
    var files by remember{mutableStateOf<List<HubModelFile>>(emptyList())}
    var fileFilter by remember{mutableStateOf("")}
    var error by remember{mutableStateOf<String?>(null)}
    Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("GET MODELS FROM HUGGING FACE",color=Gold)
            Text("Search repositories and choose an exact model file. Downloads "+
                "are commit-pinned, resume interrupted transfers, and retain "+
                "original model files. Only compatible QNN packages and verified "+
                "local GGUF weights can run immediately; other files remain available "+
                "for later conversion.",color=Mist)
            OutlinedTextField(value=query,onValueChange={query=it},
                label={Text("Search Hugging Face models")},
                singleLine=true,modifier=Modifier.fillMaxWidth())
            Button(enabled=!searching&&query.trim().length>=2,onClick={
                scope.launch {
                    searching=true;error=null;chosen=null;files=emptyList()
                    try {repositories=withContext(Dispatchers.IO){hub.search(query)}}
                    catch(e:Exception){error=e.message;repositories=emptyList()}
                    finally{searching=false}
                }
            }){Text(if(searching)"Searching…" else "Search models")}
            if(chosen==null) {
                repositories.take(30).forEach{model->
                    OutlinedButton(modifier=Modifier.fillMaxWidth(),
                        enabled=!searching&&!model.gated,onClick={
                        scope.launch {
                            searching=true;error=null
                            try {
                                files=withContext(Dispatchers.IO){hub.files(model)}
                                chosen=model
                            }catch(e:Exception){error=e.message}
                            finally{searching=false}
                        }
                    }){Text(model.id+(if(model.gated)" · gated (login required)" else ""))}
                }
            }else{
                val model=requireNotNull(chosen)
                Text(model.id,color=White)
                Text("Pinned revision: "+model.revision.take(14),color=Mist)
                OutlinedTextField(value=fileFilter,onValueChange={fileFilter=it},
                    label={Text("Filter model filenames")},singleLine=true,
                    modifier=Modifier.fillMaxWidth())
                OutlinedButton(onClick={chosen=null;files=emptyList()}){Text("Back to results")}
                if(files.isEmpty())Text("No supported model files found in this repository",color=Mist)
                val matches=files.filter{file->
                    fileFilter.isBlank()||file.path.contains(fileFilter,ignoreCase=true)
                }
                Text(matches.size.toString()+" matching file(s); showing up to 40. "+
                    "Use filename filtering for large repositories.",color=Mist)
                matches.take(40).forEach{file->
                    Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
                        Text(file.path,color=White)
                        Text(file.kind+" · "+
                            (if(file.bytes>0)"%.2f GiB".format(file.bytes/1073741824.0)
                            else "Size unknown")+
                            " · "+if(file.sha256!=null)"SHA-256 available" else
                            "Commit-pinned; no LFS SHA-256 published",color=Mist,
                            style=MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick={
                            error=runCatching{
                                val task=HuggingFaceDownloadWorker.enqueue(context,
                                    model.id,model.revision,file.path,
                                    file.bytes,file.sha256,selectedFamily)
                                "Download queued: "+task.take(12)+
                                    " · Track progress under Model imports."
                            }.getOrElse{it.message}
                        }){Text("Download / import selected file")}
                    }
                    HorizontalDivider()
                }
            }
            error?.let{Text(it,color=Gold)}
        }
    }
}
