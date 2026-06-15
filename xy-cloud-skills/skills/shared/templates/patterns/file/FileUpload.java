// === 在 ${entity}Controller.java 中添加文件上传方法 ===
@Resource
private FileApi fileApi;  // Feign 注入

/**
 * ${businessName} 文件上传
 */
@PostMapping("/upload")
@PreAuthorize("@ss.hasPermission('${module}:${resource}:create')")
public CommonResult<String> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
    String url = fileApi.createFile(file.getOriginalFilename(),
            "${module}/${resource}/", file.getBytes());
    return CommonResult.success(url);
}

// === 在 @EnableFeignClients 中注册 FileApi（如尚未注册） ===
// 在 config/ 或 framework/ 下的 RPC 配置类中：
// @EnableFeignClients(clients = {FileApi.class, ...})

// === 如需下载 ===
/**
 * 下载 ${businessName} 文件
 */
@GetMapping("/download")
@PreAuthorize("@ss.hasPermission('${module}:${resource}:query')")
public void downloadFile(@RequestParam("configId") Long configId,
                          @RequestParam("path") String path,
                          HttpServletResponse response) throws Exception {
    byte[] content = fileApi.getFileContent(configId, path);
    ServletUtils.writeAttachment(response, path, content);
}
