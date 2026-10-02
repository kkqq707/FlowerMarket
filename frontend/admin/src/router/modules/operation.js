// +----------------------------------------------------------------------
// | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
// +----------------------------------------------------------------------
// | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
// +----------------------------------------------------------------------
// | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
// +----------------------------------------------------------------------
// | Author: CRMEB Team <admin@crmeb.com>
// +----------------------------------------------------------------------

import Layout from '@/layout';

const operationRouter = {
  path: '/operation',
  component: Layout,
  redirect: '/operation/setting',
  name: 'Operation',
  meta: {
    title: '系统设置',
    icon: 'clipboard',
    roles: ['admin'],
  },
  children: [
    {
      path: 'setting',
      name: 'setting',
      component: () => import('@/views/systemSetting/setting'),
      meta: {
        title: '系统设置',
        icon: 'clipboard',
      },
    },
    {
      path: 'notification',
      name: 'notification',
      component: () => import('@/views/systemSetting/notification'),
      meta: {
        title: '消息通知',
        icon: 'clipboard',
      },
    },
    {
      path: 'roleManager',
      name: 'RoleManager',
      component: () => import('@/views/systemSetting/administratorAuthority'),
      meta: {
        title: '员工与权限',
        icon: 'clipboard',
        roles: ['admin'],
      },
      children: [
        {
          path: 'adminList',
          component: () => import('@/views/systemSetting/administratorAuthority/adminList'),
          name: 'adminList',
          meta: { title: '员工账号', icon: '' },
        },
        {
          path: 'identityManager',
          component: () => import('@/views/systemSetting/administratorAuthority/identityManager'),
          name: 'identityManager',
          meta: { title: '角色与权限', icon: '' },
        },
        {
          path: 'promiseRules',
          component: () => import('@/views/systemSetting/administratorAuthority/permissionRules'),
          name: 'promiseRules',
          meta: { title: '菜单与权限项', icon: '' },
        },
      ],
    },
    {
      path: 'agreement',
      name: 'agreement',
      component: () => import('@/views/systemSetting/agreement'),
      meta: {
        title: '协议管理',
      },
    },
  ],
};

export default operationRouter; //collate
