package vn.edu.qnu.st.nambede.auth.user.dto;

import vn.edu.qnu.st.nambede.auth.user.AccountType;

/** Account plan of a user: FREE or VIP. */
public record AccountTypeResponse(Long userId, AccountType accountType) {
}
