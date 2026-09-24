const SUPABASE_URL = 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SERVICE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZrdmVpbXB2cnBuemVsYnN2ZHJnIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4NjM1NTk1MSwiZXhwIjoyMTAxOTMxOTUxfQ.ycLj0C47iUsdA04zBs2ShXPdgGQfhaMZQTCCCg7x8_o';

async function audit() {
  const headers = {
    'apikey': SERVICE_KEY,
    'Authorization': 'Bearer ' + SERVICE_KEY,
    'Content-Type': 'application/json'
  };

  console.log('=== AUDITING user_sync_data ===');
  const res1 = await fetch(`${SUPABASE_URL}/rest/v1/user_sync_data?select=*`, { headers });
  const users = await res1.json();
  console.log(`Total user_sync_data rows: ${users.length}`);
  users.forEach((u, i) => {
    console.log(`[${i+1}] user_id: ${u.user_id}`);
    console.log(`    user_name: "${u.user_name}"`);
    console.log(`    user_email: "${u.user_email}"`);
    console.log(`    profile_status: "${u.profile_status}"`);
    console.log(`    updated_at: ${u.updated_at} (${new Date(Number(u.updated_at)).toISOString()})`);
    if (u.pending_profile_json) {
      console.log(`    pending_profile_json: ${u.pending_profile_json}`);
    }
  });

  console.log('\n=== AUDITING daily_leaderboard ===');
  const res2 = await fetch(`${SUPABASE_URL}/rest/v1/daily_leaderboard?select=*`, { headers });
  const lb = await res2.json();
  if (Array.isArray(lb)) {
    console.log(`Total daily_leaderboard rows: ${lb.length}`);
    lb.forEach((row, i) => {
      console.log(`[${i+1}] date: ${row.study_date} | user_id: ${row.user_id} | user_name: "${row.user_name}" | sec: ${row.total_duration_seconds} | mood: "${row.status_mood || ''}" | exam: "${row.exam_tag || ''}" | ring: "${row.avatar_ring || ''}"`);
    });
  } else {
    console.log('daily_leaderboard response is not an array:', lb);
  }
}

audit().catch(console.error);
