export function arrangeFlowerMenus(menus) {
  const menuTitles = {
    136: '角色与权限',
    138: '菜单与权限项',
    439: '商城装饰',
    578: '花卉经营',
  };
  const hideCouponMenus = (items) => items
    .filter((item) =>
      !/coupon/i.test(item.url || item.component || item.path || '') &&
      !/优惠[券劵]/.test(item.name || item.title || ''))
    .map((item) => {
      const title = menuTitles[Number(item.id)];
      return {
        ...item,
        ...(title && item.name ? { name: title } : {}),
        ...(title && item.title ? { title } : {}),
        ...(item.child ? { child: hideCouponMenus(item.child) } : {}),
        ...(item.children ? { children: hideCouponMenus(item.children) } : {}),
        ...(item.childList ? { childList: hideCouponMenus(item.childList) } : {}),
      };
    });
  const childKey = (item) => ['childList', 'children', 'child'].find((key) => Array.isArray(item[key]));
  const arranged = hideCouponMenus(menus);

  const peopleIndex = arranged.findIndex((menu) => Number(menu.id) === 4);
  const settingsIndex = arranged.findIndex((menu) => Number(menu.id) === 12);
  if (peopleIndex !== -1 && settingsIndex !== -1) {
    const people = arranged[peopleIndex];
    const settings = arranged[settingsIndex];
    const peopleKey = childKey(people);
    const settingsKey = childKey(settings);
    const roleManager = settings[settingsKey]?.find((item) => Number(item.id) === 123);
    const roleKey = roleManager && childKey(roleManager);
    if (peopleKey && settingsKey && roleKey) {
      const roleChildren = roleManager[roleKey];
      const staff = [137, 136]
        .map((id) => roleChildren.find((item) => Number(item.id) === id))
        .filter(Boolean);
      const technical = roleChildren.find((item) => Number(item.id) === 138);
      const movedIds = new Set([...staff, technical].filter(Boolean).map((item) => Number(item.id)));
      const remaining = roleChildren.filter((item) => !movedIds.has(Number(item.id)));
      const existingPeopleIds = new Set(people[peopleKey].map((item) => Number(item.id)));
      arranged[peopleIndex] = {
        ...people,
        [peopleKey]: [
          ...people[peopleKey],
          ...staff.filter((item) => !existingPeopleIds.has(Number(item.id))).map((item) => ({ ...item, pid: 4 })),
        ],
      };
      arranged[settingsIndex] = {
        ...settings,
        [settingsKey]: [
          ...settings[settingsKey].flatMap((item) => Number(item.id) === 123
            ? (remaining.length ? [{ ...item, [roleKey]: remaining }] : [])
            : [item]),
          ...(technical ? [{ ...technical, pid: 12 }] : []),
        ],
      };
    }
  }
  const flowerIndex = arranged.findIndex((menu) => Number(menu.id) === 578);
  const productIndex = arranged.findIndex((menu) => Number(menu.id) === 2);
  if (flowerIndex !== -1 && productIndex !== -1 && flowerIndex > productIndex) {
    arranged.splice(productIndex, 0, arranged.splice(flowerIndex, 1)[0]);
  }
  return arranged;
}
