import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
    baseURL: 'http://localhost:9090',
    timeout: 30000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    }
});
const loadImage = (imageUrl) => {
    const token = localStorage.getItem('jwt');
    return axios.get(imageUrl, {
        headers: {
            'Authorization': token,
        },
        responseType: 'blob'  // 重要：将响应类型设置为blob，适用于图片加载
    });
}
// ✅ 请求拦截器 —— 每次请求自动携带 jwt
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('jwt');
        if (token) {
            config.headers['Authorization'] = token;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// ✅ 响应拦截器 —— 统一处理响应，包括 401 跳转
request.interceptors.response.use(
    response => {
        let res = response.data;
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res;
        }

        // 如果后端返回新的 token（例如刷新机制），可以更新本地
        const token = response.headers['authorization'];
        if (token) {
            localStorage.setItem('jwt', token);
        }

        return res;
    },
    error => {
        const { response } = error;

        if (!response) {
            ElMessage.error('无法连接到服务器');
            return Promise.reject(error);
        }

        const { status, data } = response;

        if (status === 401) {
            ElMessage.error('登录状态失效，请重新登录');
            localStorage.removeItem('jwt'); // 清除本地 token
            window.location.href = '/login'; // 跳转到登录页
        } else if (status === 404) {
            ElMessage.error('接口不存在');
        } else if (status === 500) {
            ElMessage.error('服务器错误');
        } else {
            ElMessage.error(data?.message || '请求异常');
        }

        return Promise.reject(error);
    }
);

export default request;
