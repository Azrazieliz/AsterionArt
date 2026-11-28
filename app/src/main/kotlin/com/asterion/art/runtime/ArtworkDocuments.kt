package com.asterion.art.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

internal const val ROUNDER_DOCUMENTS = "com.openlocal.office.terabox.documents"

internal data class ArtworkDocument(val id: String, val name: String, val directory: Boolean)

internal data class ArtworkPage(
    val entries: List<ArtworkDocument>,
    val loading: Boolean,
    val nextOffset: Int?,
)

internal class ArtworkDocuments(context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("artwork-documents", Context.MODE_PRIVATE)

    fun selectedTree(): Uri? {
        val tree = preferences.getString("tree", null)?.let(Uri::parse) ?: return null
        return tree.takeIf { root -> resolver.persistedUriPermissions.any { it.uri == root && it.isReadPermission } }
    }

    fun select(tree: Uri) {
        require(DocumentsContract.isTreeUri(tree) && tree.authority in setOf(ROUNDER_DOCUMENTS, NEXUS_DOCUMENTS)) {
            "Choose the artwork folder in Rounder or Nexus"
        }
        resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        check(preferences.edit().putString("tree", tree.toString()).commit()) { "Could not remember the artwork folder" }
    }

    fun page(tree: Uri, folder: String, offset: Int): ArtworkPage {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folder)
        val args = Bundle().apply {
            putInt("asterion.page.offset", offset)
            putInt("asterion.page.limit", PAGE_SIZE)
        }
        val rows = checkNotNull(resolver.query(children, COLUMNS, args, null)) {
            "The artwork folder could not be listed"
        }
        return rows.use {
            val extras = rows.extras
            val loading = extras.getBoolean(DocumentsContract.EXTRA_LOADING) ||
                (extras.containsKey("asterion.page.ready") && !extras.getBoolean("asterion.page.ready"))
            if (loading) return@use ArtworkPage(emptyList(), loading = true, nextOffset = null)
            val paged = extras.containsKey("asterion.page.ready")
            check(paged || offset == 0) { "The artwork provider's paging mode changed. Reopen the folder." }
            val entries = buildList {
                while (rows.moveToNext()) {
                    val type = rows.getString(2).orEmpty()
                    val directory = type == Document.MIME_TYPE_DIR
                    if (directory || type.startsWith("image/")) {
                        add(ArtworkDocument(rows.getString(0), rows.getString(1), directory))
                    }
                }
            }
            val nextOffset = if (paged && rows.count == PAGE_SIZE) Math.addExact(offset, PAGE_SIZE) else null
            ArtworkPage(entries, loading = false, nextOffset = nextOffset)
        }
    }

    fun uri(tree: Uri, document: ArtworkDocument): Uri {
        require(!document.directory)
        return DocumentsContract.buildDocumentUriUsingTree(tree, document.id)
    }

    companion object {
        const val PAGE_SIZE = 100
        private val COLUMNS = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
    }
}
