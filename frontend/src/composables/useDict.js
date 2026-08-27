import { reactive } from 'vue'
import request from '../utils/request'

const cache = {}

function normalize(rows) {
  return rows.map((r) => ({ label: r.dictLabel, value: r.dictValue, elTagType: r.listClass || '' }))
}

export function useDict(...types) {
  const result = {}
  types.forEach((type) => {
    result[type] = reactive({ value: [] })
    if (cache[type]) {
      result[type].value = cache[type]
      return
    }
    request.post('/rest/sysDictData/list', { dictType: type }).then((rows) => {
      const normalized = normalize(rows)
      cache[type] = normalized
      result[type].value = normalized
    })
  })
  return result
}
