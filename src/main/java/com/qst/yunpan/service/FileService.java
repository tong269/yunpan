package com.qst.yunpan.service;

import com.qst.yunpan.dao.UserDao;
import com.qst.yunpan.dao.OfficeDao;
import com.qst.yunpan.dao.FileDao;
import com.qst.yunpan.pojo.FileCustom;
import com.qst.yunpan.pojo.User;
import com.qst.yunpan.utils.FileUtils;
import com.qst.yunpan.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {
    //文件相对前缀
    public static final String PREFIX = "WEB-INF" + File.separator + "file" + File.separator;
    //新用户注册默认文件夹
    public static final String[] DEFAULT_DIRECTORY = { "vido", "music", "source", "image", User.RECYCLE };
    @Autowired
    private UserDao userDao;
    @Autowired
    private OfficeDao officeDao;
    @Autowired
    private FileDao fileDao;

    /**
     * 上传文件至前端页面中的当前路径
     * @param request
     * @param files
     * @param currentPath
     * @throws Exception
     */
    public void uploadFilePath(HttpServletRequest request, MultipartFile[] files, String currentPath) throws Exception {
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            String filePath = getFileName(request, currentPath);
            File distFile = new File(filePath, fileName);
            if (!distFile.exists()) {
                file.transferTo(distFile);
                if ("office".equals(FileUtils.getFileType(distFile))) {
                    try {
                        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
                        String documentId = FileUtils.getDocClient().createDocument(distFile, fileName, suffix).getDocumentId();
                        officeDao.addOffice(documentId, FileUtils.MD5(distFile));
                    } catch (Exception e) {
                    }
                }
            }
        }
        reSize(request);//reSize的定义与实现在下一个任务点完成
    }



    public void addNewNameSpace(HttpServletRequest request, String namespace) {
        String fileName = getRootPath(request);
        File file = new File(fileName, namespace);
        file.mkdir();
        for (String newFileName : DEFAULT_DIRECTORY) {
            File newFile = new File(file, newFileName);
            newFile.mkdir();
        }
    }
    public String getRootPath(HttpServletRequest request) {
        String rootPath = request.getSession().getServletContext().getRealPath("/") + PREFIX;
        return rootPath;
    }

    /**
     * 获取文件路径
     *
     * @param request
     * @param fileName
     * @return
     */
    public String getFileName(HttpServletRequest request, String fileName) {
        fileName= fileName.replace("\\", "//");
        if (fileName == null||fileName.equals("\\")) {
            System.out.println(1);
            fileName = "";
        }
        String username = UserUtils.getUsername(request);
        String realpath=getRootPath(request) + username + File.separator + fileName;
        return realpath;
    }

    /**
     * 根据用户名获取文件路径
     *
     * @param request
     * @param fileName
     * @param username
     * @return
     */
    public String getFileName(HttpServletRequest request, String fileName, String username){
        if (username == null){
            return getFileName(request, fileName);
        }
        if(fileName == null){
            fileName = "";
        }
        return getRootPath(request) + username + File.separator + fileName;
    }

    public List<FileCustom> listFile() {
        return listFile();
    }

    public List<FileCustom> listFile(String realPath) {
        File[] files = new File(realPath).listFiles();
        List<FileCustom> lists = new ArrayList<FileCustom>();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().equals(User.RECYCLE)) {
                    FileCustom custom = new FileCustom();
                    custom.setFileName(file.getName());
                    custom.setLastTime(FileUtils.formatTime(file.lastModified()));
                    custom.setCurrentPath(realPath);
                    if (file.isDirectory()) {
                        custom.setFileSize("-");
                    } else {
                        custom.setFileSize(FileUtils.getDataSize(file.length()));
                    }
                    custom.setFileType(FileUtils.getFileType(file));
                    lists.add(custom);
                }
            }
        }
        return lists;
    }

    /**
     * 重新计算文件大小
     *
     * @param request
     */
    public void reSize (HttpServletRequest request) {
        String userName = UserUtils.getUsername(request);
        try {
            userDao.reSize(userName, countFileSize(request));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 统计用户文件大小
     *
     * @param request
     * @return
     */
    public String countFileSize(HttpServletRequest request) {
        long countFileSize = countFileSize(new File(getFileName(request, null)));
        return FileUtils.getDataSize(countFileSize);
    }
    private long countFileSize(File srcFile) {
        File[] listFiles = srcFile.listFiles();
        if (listFiles == null) {
            return 0;
        }
        long count = 0;
        for (File file : listFiles) {
            if (file.isDirectory()) {
                count += countFileSize(file);
            } else {
                count += file.length();
            }
        }
        return count;
    }

    public File downPackage(HttpServletRequest request, String currentPath, String[] fileNames, String username) throws Exception {
        File downloadFile = null;
        if (currentPath == null) {
            currentPath = "";
        }
        //单文件length为1
        if (fileNames.length == 1) {
            downloadFile = new File(getFileName(request, currentPath, username), fileNames[0]);
            if (downloadFile.isFile()) {
                return downloadFile;
            }
        }
        String[] sourcePath = new String[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            sourcePath[i] = getFileName(request, currentPath, username) + File.separator + fileNames[i];
        }
        String packageZipName = packageZip(sourcePath);
        downloadFile = new File(packageZipName);
        return downloadFile;
    }

    /**
     * 压缩文件
     *
     * @param sourcePath
     * @return
     * @throws Exception
     */
    private String packageZip(String[] sourcePath) throws Exception {
        String zipName = sourcePath[0] + (sourcePath.length == 1 ? "" : "等" + sourcePath.length + "个文件") + ".zip";
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipName));
            for (String string : sourcePath) {
                writeZos(new File(string), "", zos);
            }
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
        return zipName;
    }

    /**
     * 写入文件到压缩包
     *
     * @param file
     * @param basePath
     * @param zos
     * @throws IOException
     */
    private void writeZos(File file, String basePath, ZipOutputStream zos) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles.length != 0) {
                for (File childFile : listFiles) {
                    writeZos(childFile, basePath + file.getName() + File.separator, zos);
                }
            }
        } else {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            ZipEntry entry = new ZipEntry(basePath + file.getName());
            zos.putNextEntry(entry);
            int count = 0;
            byte data[] = new byte[1024];
            while ((count = bis.read(data)) != -1) {
                zos.write(data, 0, count);
            }
            bis.close();
        }
    }

    /**
     * 删除压缩文件包
     *
     * @param downloadFile
     */
    public void deleteDownPackage(File downloadFile) {
        if (downloadFile.getName().endsWith("个文件.zip")) {
            downloadFile.delete();
        }
    }

    /**
     * 查找文件
     *
     * @param request
     * @param currentPath
     *            当前路径
     * @param regType
     *            文件类型
     * @return
     */
    public List<FileCustom> searchFile(HttpServletRequest request, String currentPath, String regType) {
        List<FileCustom> list = new ArrayList<>();
        matchFile(request, list, new File(getSearchFileName(request, currentPath)), regType == null ? "" : regType);
        return list;
    }

    private String getSearchFileName(HttpServletRequest request, String fileName) {
        if (fileName == null||fileName.equals("\\")) {
            System.out.println(1);
            fileName = "";
        }
        String username = UserUtils.getUsername(request);
        String realpath=getRootPath(request) + username + File.separator + fileName;
        return realpath;
    }

    private void matchFile(HttpServletRequest request, List<FileCustom> list, File dirFile, String regType) {
        File[] listFiles = dirFile.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isFile()) {
                    String suffixType = FileUtils.getFileType(file);
                    if (suffixType.equals(regType)) {
                        FileCustom custom = new FileCustom();
                        custom.setFileName(file.getName());
                        custom.setLastTime(FileUtils.formatTime(file.lastModified()));
                        String parentPath = file.getParent();
                        String prePath = parentPath.substring(
                                parentPath.indexOf(getSearchFileName(request, null)) + getSearchFileName(request, null).length());
                        custom.setCurrentPath(File.separator + prePath);
                        if (file.isDirectory()) {
                            custom.setFileSize("-");
                        } else {
                            custom.setFileSize(FileUtils.getDataSize(file.length()));
                        }
                        custom.setFileType(FileUtils.getFileType(file));
                        list.add(custom);
                    }
                } else {
                    matchFile(request, list, file, regType);
                }
            }
        }
    }

    /**
     * 新建文件夹
     *
     * @param request
     * @param currentPath
     *            当前路径
     * @param directoryName
     *            文件夹名
     * @return
     */
    public boolean addDirectory(HttpServletRequest request, String currentPath, String directoryName) {
        File file = new File(getFileName(request, currentPath), directoryName);
        return file.mkdir();
    }

    /**
     * 删除文件
     *
     * @param request
     * @param currentPath
     *            当前路径
     * @param directoryName
     *            文件名
     * @throws Exception
     */
    public void delDirectory(HttpServletRequest request, String currentPath, String[] directoryName) throws Exception {
        for (String fileName : directoryName) {
            //拼接源文件的地址
            String srcPath = currentPath + File.separator + fileName;
            //根据源文件相对地址拼接 绝对路径
            File src = new File(getFileName(request, srcPath));//即将删除的文件地址
            File dest = new File(getRecyclePath(request));//回收站目录地址
            //调用commons.jar包中的moveToDirectory移动文件,移至回收站目录
            org.apache.commons.io.FileUtils.moveToDirectory(src, dest, true);
            //保存本条删除信息
            fileDao.insertFiles(srcPath, UserUtils.getUsername(request));
        }
        //重新计算文件大小
        reSize(request);
    }
    public String getRecyclePath(HttpServletRequest request) {
        return getFileName(request, User.RECYCLE);
    }
}
