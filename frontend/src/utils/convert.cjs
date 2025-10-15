const fs = require('fs');

// 读取 PCM 文件并转换为 Base64
const filePath = 'iat_pcm_16k.pcm'; // 替换为你的文件路径
const pcmData = fs.readFileSync(filePath);

// 转换为 Base64 编码
const base64Data = pcmData.toString('base64');
console.log(base64Data);
