package cn.yangwanhao.billapp.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileUtils {

    /**
     * 计算文件的 MD5 值
     */
    fun getFileMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val md5Bytes = digest.digest()
        return md5Bytes.joinToString("") { "%02x".format(it) }
    }
}