val parsedCapture = VoiceParser.parse(recognizedText)
if (parsedCapture != null) {
    // Guardar en base de datos
}

val queryName = VoiceParser.parseQuery(recognizedText)
if (queryName != null) {
    // Mostrar consulta deuda para ese nombre
}
