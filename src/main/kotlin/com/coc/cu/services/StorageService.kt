package com.coc.cu.services

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*


@Service
class StorageService(val amazonS3: AmazonS3) {

    @Value("\${app.awsServices.bucketName}")
    private val bucketName: String? = null

    @Value("\${cloud.aws.region}")
    private val region: String? = null

    fun uploadMultipartFile(file: MultipartFile): String {

        val meta = ObjectMetadata()
        meta.contentType = file.contentType
        meta.contentLength = file.size

        val filename = UUID.randomUUID().toString() + file.contentType?.split("/")?.get(1)
        val request = PutObjectRequest(
            bucketName,
            filename,
            file.inputStream,
            meta
        )

        amazonS3.putObject(request.withCannedAcl(CannedAccessControlList.PublicRead))
        return String.format("https://%s.linodeobjects.com/%s/%s", region, bucketName, filename)
    }
}