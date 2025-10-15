const crypto = require('crypto')

// ====== 下面三样东西改成你的 ======
const apiKey = '9ba11557d60ead2ce2fcd88f3449cd24'
const apiSecret = 'NDA4NTM0YzAwMTQyYTA5NmYwYTA2N2Iz'
// ===================================

const host = 'iat-api.xfyun.cn'
const requestLine = 'GET /v2/iat HTTP/1.1'

// 获取当前UTC时间（RFC1123格式）
const date = new Date().toUTCString()

// 拼接signature_origin字符串
const signatureOrigin = `host: ${host}\ndate: ${date}\n${requestLine}`

// 用APISecret进行HMAC-SHA256签名
const signatureSha = crypto
    .createHmac('sha256', apiSecret)
    .update(signatureOrigin)
    .digest('base64')

// 再拼接authorization_origin
const authorizationOrigin = `api_key="${apiKey}", algorithm="hmac-sha256", headers="host date request-line", signature="${signatureSha}"`

// Base64编码
const authorization = Buffer.from(authorizationOrigin).toString('base64')

// 最后拼接WebSocket链接
const wsUrl = `wss://${host}/v2/iat?authorization=${authorization}&date=${encodeURIComponent(date)}&host=${host}`

console.log('生成的讯飞 WebSocket URL：')
console.log(wsUrl)
