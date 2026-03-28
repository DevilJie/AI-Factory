/**
 * 格式化数字显示
 * @param num 数字
 * @returns 格式化后的字符串
 */
export function formatNumber(num: number | string): string {
  if (num < 1000) return num.toString()
  if (num < 10000) return (num / 1000).toFixed(1) + 'k'
  if (num < 100000000) return (num / 10000).toFixed(1) + '万'
  return (num / 100000000).toFixed(1) + '亿'
}
