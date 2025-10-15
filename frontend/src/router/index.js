import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {path: '/testforge', meta:{title:'导航页面'}, component:()=>import('../views/Manager.vue'), children:[
        {path: 'qa', name: 'qa', meta:{title:'智能问答'},component: () => import('../views/QA.vue')},
        {path: 'testCase',name: 'testCase',meta: { title: '用例管理' },component: () => import('../views/TestCase.vue')},
        {path: 'organization', name: 'organization', meta:{title:'组织'},component: () => import('../views/Organization.vue')},
        {path: 'department', name: 'department', meta:{title:'部门'},component: () => import('../views/Department.vue')},
        {path: 'permission', name: 'permission', meta:{title:'角色权限'},component: () => import('../views/Permission.vue')},
        {path: 'personInfo', name: 'personInfo', meta:{title:'个人信息'},component: () => import('../views/PersonInfo.vue')},
        {path: 'password', name: 'password', meta:{title:'修改密码'},component: () => import('../views/password.vue')},
        {path: 'schedule', name: 'schedule', meta:{title:'我的日程'},component: () => import('../views/schedule.vue')},
      ]},
    {path: '/',redirect:'/login'},
    {path: '/404',name :'NotFound',meta:{title: '走丢了~'},component:()=> import('../views/404.vue')},
    {path: '/login',name :'login',meta:{title: '登陆界面'},component:()=> import('../views/login.vue')},
    {path: '/register',name :'register',meta:{title: '注册界面'},component:()=> import('../views/register.vue')},
    {path:'/:pathMatch(.*)',redirect: '/404'}
  ]
})

router.beforeEach((to,from,next)=>{
    document.title = to.meta.title
    next()
})

export default router
