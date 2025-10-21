// src/plugins/permDirective.js
import { hasPermission } from '@/utils/perm';

export default {
    install(app) {
        app.directive('perm', {
            mounted(el, binding) { apply(el, binding); },
            updated(el, binding) { apply(el, binding); }
        });

        function apply(el, binding) {
            const val = binding.value;
            let name, hide;
            if (typeof val === 'string') {
                name = val;
                hide = true;
            } else if (val && typeof val === 'object') {
                name = val.name;
                hide = val.hide !== undefined ? val.hide : true;
            } else {
                el.style.display = 'none';
                return;
            }

            const ok = hasPermission(name);

            if (hide) {
                el.style.display = ok ? '' : 'none';
            } else {
                // 尽量支持按钮/链接的禁用
                if (el.tagName === 'BUTTON' || el.getAttribute('role') === 'button' || el.disabled !== undefined) {
                    el.disabled = !ok;
                    if (!ok) el.classList.add('disabled-by-perm');
                    else el.classList.remove('disabled-by-perm');
                } else {
                    el.style.pointerEvents = ok ? '' : 'none';
                    el.style.opacity = ok ? '' : '0.55';
                }
            }
        }
    }
};
