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

function getStoredToken() {
    return localStorage.getItem('auth.token') || localStorage.getItem('jwt') || null;
}
function saveTokenFromHeader(headerToken) {
    if (!headerToken) return;
    const normalized = headerToken.startsWith('Bearer ') ? headerToken.substring(7) : headerToken;
    localStorage.setItem('auth.token', normalized);
}
function toAuthHeader(token) {
    if (!token) return undefined;
    return `Bearer ${token}`;
}

request.interceptors.request.use(
    config => {
        const token = getStoredToken();
        if (token) {
            config.headers['Authorization'] = toAuthHeader(token);
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

export const loadImage = (imageUrl) => {
    const token = getStoredToken();
    const headers = {};
    if (token) headers['Authorization'] = toAuthHeader(token);
    return request.get(imageUrl, {
        headers,
        responseType: 'blob'
    });
};

request.interceptors.response.use(
    response => {
        let res = response.data;

        if (typeof res === 'string') {
            try {
                res = res ? JSON.parse(res) : res;
            } catch (e) {
            }
        }

        const returnedAuth = response.headers && (response.headers['authorization'] || response.headers['Authorization']);
        if (returnedAuth) {
            saveTokenFromHeader(returnedAuth);
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
            localStorage.removeItem('auth.token'); // 清除本地 token
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
