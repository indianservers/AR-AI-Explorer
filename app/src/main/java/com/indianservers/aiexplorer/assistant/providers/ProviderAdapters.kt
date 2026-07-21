package com.indianservers.aiexplorer.assistant.providers

import com.indianservers.aiexplorer.assistant.contracts.*
import com.indianservers.aiexplorer.assistant.privacy.SecureSecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ProviderPayload(val providerId: String, val systemInstruction: String, val learnerText: String, val groundedFacts: List<String>, val maximumOutputCharacters: Int = 1600)
fun interface ProviderTransport { suspend fun execute(payload: ProviderPayload): String }

abstract class GroundedRemoteProvider(final override val id: String, private val transport: ProviderTransport) : LearningAssistantProvider {
    override val capabilities = AssistantCapability.entries.toSet()
    final override suspend fun respond(request: GroundedAssistantRequest): AssistantResponse {
        val facts = request.verifiedContent.filter { it.reviewed }.map { "${it.id}: ${it.text}" }
        val text = transport.execute(ProviderPayload(id, "Use only supplied reviewed facts. Ask one useful question. Never change formulas, values, curriculum status, or safety constraints.", request.learnerQuestion, facts))
        val cited = request.verifiedContent.filter { block -> text.contains(block.text.take(24), ignoreCase = true) }.map { it.id }.ifEmpty { request.verifiedContent.take(1).map { it.id } }
        return AssistantResponse(text, AssistantResponseType.EXPLANATION, cited, emptyList(), AssistantVerificationStatus.PARTIALLY_VERIFIED, emptyList(), id)
    }
}
class GroqAssistantProvider(transport: ProviderTransport) : GroundedRemoteProvider("groq", transport)
class OpenAiAssistantProvider(transport: ProviderTransport) : GroundedRemoteProvider("openai", transport)
class AnthropicAssistantProvider(transport: ProviderTransport) : GroundedRemoteProvider("anthropic", transport)

data class ProviderHttpSpec(val id:String,val endpoint:String,val model:String,val anthropicFormat:Boolean=false)
object ProviderHttpSpecs{
    val groq=ProviderHttpSpec("groq","https://api.groq.com/openai/v1/chat/completions","llama-3.3-70b-versatile")
    val openAi=ProviderHttpSpec("openai","https://api.openai.com/v1/responses","gpt-5")
    val anthropic=ProviderHttpSpec("anthropic","https://api.anthropic.com/v1/messages","claude-sonnet-4-20250514",true)
}

/** Minimal dependency-free transport. It never logs prompts, responses, or credentials. */
class SecureHttpProviderTransport(private val spec:ProviderHttpSpec,private val secrets:SecureSecretStore):ProviderTransport{
    override suspend fun execute(payload:ProviderPayload):String=withContext(Dispatchers.IO){
        val secret=secrets.get(spec.id)?:error("No secure API key is stored for ${spec.id}.")
        try{
            val connection=(URL(spec.endpoint).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15_000;readTimeout=30_000;doOutput=true;setRequestProperty("Content-Type","application/json");if(spec.anthropicFormat){setRequestProperty("x-api-key",String(secret));setRequestProperty("anthropic-version","2023-06-01")}else setRequestProperty("Authorization","Bearer ${String(secret)}")}
            val grounded=(listOf(payload.systemInstruction)+payload.groundedFacts+payload.learnerText).joinToString("\n")
            val body=when{spec.anthropicFormat->"{\"model\":\"${json(spec.model)}\",\"max_tokens\":400,\"system\":\"${json(payload.systemInstruction)}\",\"messages\":[{\"role\":\"user\",\"content\":\"${json(payload.groundedFacts.joinToString("\n")+"\nQuestion: "+payload.learnerText)}\"}]}";spec.id=="openai"->"{\"model\":\"${json(spec.model)}\",\"instructions\":\"${json(payload.systemInstruction)}\",\"input\":\"${json(payload.groundedFacts.joinToString("\n")+"\nQuestion: "+payload.learnerText)}\",\"max_output_tokens\":400,\"store\":false}";else->"{\"model\":\"${json(spec.model)}\",\"messages\":[{\"role\":\"system\",\"content\":\"${json(payload.systemInstruction)}\"},{\"role\":\"user\",\"content\":\"${json(grounded)}\"}],\"max_completion_tokens\":400,\"temperature\":0.2}"}
            connection.outputStream.use{it.write(body.toByteArray())}
            val status=connection.responseCode;val raw=(if(status in 200..299)connection.inputStream else connection.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();if(status !in 200..299)error("Provider connection failed with HTTP $status")
            extractText(raw,spec)
        }finally{secret.fill('\u0000')}
    }
    private fun extractText(raw:String,spec:ProviderHttpSpec):String{
        val patterns=if(spec.id=="openai")listOf(Regex("\\\"output_text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)"),Regex("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)"))else if(spec.anthropicFormat)listOf(Regex("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)"))else listOf(Regex("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)"));val encoded=patterns.firstNotNullOfOrNull{it.find(raw)?.groupValues?.get(1)}?:error("Provider returned no text");return encoded.replace("\\n","\n").replace("\\\"","\"").replace("\\\\","\\").take(1600)
    }
    private fun json(value:String)=buildString{value.forEach{c->when(c){'\\'->append("\\\\");'\"'->append("\\\"");'\n'->append("\\n");'\r'->append("\\r");'\t'->append("\\t");else->append(c)}}}
}

object RemoteProviderFactory{
    fun providers(secrets:SecureSecretStore):Map<String,LearningAssistantProvider>{val groq=GroqAssistantProvider(SecureHttpProviderTransport(ProviderHttpSpecs.groq,secrets));val openAi=OpenAiAssistantProvider(SecureHttpProviderTransport(ProviderHttpSpecs.openAi,secrets));val anthropic=AnthropicAssistantProvider(SecureHttpProviderTransport(ProviderHttpSpecs.anthropic,secrets));return listOf(groq,openAi,anthropic).associateBy{it.id}}
}
