// src/utils/perm.js
const KEY = 'app_permissions';
let _cache = null;

export function savePermissions(permArray) {
    if (!Array.isArray(permArray)) permArray = [];
    const normalized = Array.from(new Set(permArray
        .filter(p => p != null)
        .map(p => String(p).trim().toLowerCase())
    ));
    localStorage.setItem(KEY, JSON.stringify(normalized));
    _cache = normalized;
}

/** 读权限（内存缓存优先） */
export function loadPermissions() {
    if (_cache) return _cache;
    try {
        const raw = localStorage.getItem(KEY);
        _cache = raw ? JSON.parse(raw) : [];
    } catch (e) {
        _cache = [];
    }
    return _cache;
}

/** 清除权限（登出时调用） */
export function clearPermissions() {
    _cache = null;
    localStorage.removeItem(KEY);
}

/** 单权限检查（小写规范） */
export function hasPermission(perm) {
    if (!perm) return false;
    const normalized = String(perm).trim().toLowerCase();
    const perms = loadPermissions();
    return perms.includes(normalized);
}

/** 任意（OR） */
export function hasAny(perms) {
    if (!Array.isArray(perms)) perms = [perms];
    return perms.some(p => hasPermission(p));
}

/** 全部（AND） */
export function hasAll(perms) {
    if (!Array.isArray(perms)) perms = [perms];
    return perms.every(p => hasPermission(p));
}
