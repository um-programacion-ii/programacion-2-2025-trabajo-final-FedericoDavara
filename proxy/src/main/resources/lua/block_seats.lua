-- KEYS[1] = hash key 'evento:{id}:asientos'
-- ARGV[1] = usuario
-- ARGV[2] = sessionId
-- ARGV[3] = blockedUntilIso
-- ARGV[4..n] = seats like "2:3"

local key = KEYS[1]
local usuario = ARGV[1]
local sessionId = ARGV[2]
local blockedUntil = ARGV[3]

local seats = {}
for i=4,#ARGV do seats[#seats+1]=ARGV[i] end

local failed = {}
for i,seat in ipairs(seats) do
  local val = redis.call('HGET', key, seat)
  if val ~= false and val ~= nil then
    local obj = cjson.decode(val)
    if obj.estado == 'VENDIDO' then
      table.insert(failed, {seat=seat, estado='Ocupado'})
    elseif obj.estado == 'BLOQUEADO' then
      table.insert(failed, {seat=seat, estado='Bloqueado'})
    end
  end
end

if #failed > 0 then
  return cjson.encode({resultado=false, failed=failed})
end

for i,seat in ipairs(seats) do
  local obj = {estado='BLOQUEADO', blockedBy=usuario, sessionId=sessionId, blockedUntil=blockedUntil, ventaId=nil}
  redis.call('HSET', key, seat, cjson.encode(obj))
end

return cjson.encode({resultado=true, seats=seats, blockedUntil=blockedUntil})
