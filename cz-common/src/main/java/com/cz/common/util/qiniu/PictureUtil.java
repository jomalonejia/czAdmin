package com.cz.common.util.qiniu;

import com.cz.common.util.constant.QiniuConstant;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.BatchStatus;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Created by jomalone_jia on 2017/7/25.
 */
public class PictureUtil {

    private static Logger _log = LoggerFactory.getLogger(PictureUtil.class);

    Configuration cfg = new Configuration(Zone.zone0());
    //...其他参数参考类注释
    UploadManager uploadManager = new UploadManager(cfg);
    //...生成上传凭证，然后准备上传
    String accessKey = "geOVmGh_6pimqcr-AVkh_pL_t6DYrQOUJ-Fzidl0";
    String secretKey = "Fe6gPfNFQCiyEG6f1gfgq6KgvV-GrAW209oTtAvl";
    String bucket = "jomalone-jia";
    //默认不指定key的情况下，以文件内容的hash值作为文件名
    String key = null;
    Auth auth = Auth.create(accessKey, secretKey);


    private PictureUtil() {
    }

    private static volatile PictureUtil pictureUtil;

    public static PictureUtil getInstance() {
        if (pictureUtil == null) {
            synchronized (PictureUtil.class) {
                if (pictureUtil == null) {
                    pictureUtil = new PictureUtil();
                }
            }
        }
        return pictureUtil;
    }

    public String uploadPicture(MultipartFile file) {
        DefaultPutRet putRet = null;
        try {
            String upToken = auth.uploadToken(bucket);
            try {
                Response response = uploadManager.put(file.getInputStream(), key, upToken, null, null);
                //解析上传成功的结果
                putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            } catch (QiniuException ex) {
                Response r = ex.response;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            return QiniuConstant.QINIU_BASE_URL+putRet.hash;
        }
    }

    public String uploadPicture(String uploadString) {
        String picHash = null;
        try {
            picHash = Generate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "http://upload.qiniu.com/putb64/" + getBase64FileLength(uploadString) + "/key/" + UrlSafeBase64.encodeToString(picHash);
        //非华东空间需要根据注意事项 1 修改上传域名
        RequestBody rb = RequestBody.create(null, getBasicBase64String(uploadString));
        Request request = new Request.Builder().
                url(url).
                addHeader("Content-Type", "application/octet-stream")
                .addHeader("Authorization", "UpToken " + getUpToken())
                .post(rb).build();
        OkHttpClient client = new OkHttpClient();
        try {
            okhttp3.Response response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return picHash;
    }


    private String getUpToken() {
        return auth.uploadToken(bucket, null, 3600, new StringMap().put("insertOnly", 1));
    }

    public Object deletePicture(String picHash) {
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            Response response = bucketManager.delete(bucket, picHash);
            return response;
        } catch (QiniuException ex) {
            return ex.response.toString();
        }
    }

    private int getBase64FileLength(String uploadString) {
        String fileString = uploadString.substring(uploadString.indexOf(",") + 1);
        int index = fileString.indexOf("=");
        if (index != -1) {
            fileString = fileString.substring(0, index);
        }
        double fileLength = fileString.length();
        int length = (int) Math.floor(fileLength - (fileLength / 8) * 2);
        return length;
    }

    private String getBasicBase64String(String originalString) {
        return originalString.substring(originalString.indexOf(",") + 1);
    }


    private String Generate() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(String.valueOf(new Date().getTime()).getBytes(StandardCharsets.UTF_8));
        return DatatypeConverter.printHexBinary(hash);
    }


    public void bucketDelete(String[] imageArray){
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
            batchOperations.addDeleteOp(bucket, imageArray);
            Response response = bucketManager.batch(batchOperations);
            /*BatchStatus[] batchStatusList = response.jsonToObject(BatchStatus[].class);
            for (int i = 0; i < imageArray.length; i++) {
                BatchStatus status = batchStatusList[i];
                String key = imageArray[i];
                System.out.print(key + "\t");
                if (status.code == 200) {
                    System.out.println("delete success");
                } else {
                    System.out.println(status.data.error);
                }
            }*/
        } catch (QiniuException ex) {
            System.err.println(ex.response.toString());
        }
    }

}
