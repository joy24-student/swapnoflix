package com.example

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage

val supabase = createSupabaseClient(
    supabaseUrl = "https://lgajlouffvzlccshggbn.supabase.co",
    supabaseKey = "sb_publishable__07aHkJvj4pOH837lTLXzQ_xI5g4uOC"
) {
    install(Postgrest)
    install(Auth)
    install(Storage)
}
